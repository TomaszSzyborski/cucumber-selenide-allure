package microservice.helper;

import com.jcraft.jsch.*;


import java.util.Arrays;
import java.util.Vector;

import static microservice.helper.SeleniumHelper.printMethodName;


public class SFTPService {

    static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(
            SFTPService.class.getName());

    public ChannelSftp channelSftp;

    public SFTPService(String user, String host, int port, String sshPrivatekeyFilePath) {

        try {
            JSch jsch = new JSch();

            jsch.addIdentity(sshPrivatekeyFilePath);
            System.out.println("identity added ");

            Session session = jsch.getSession(user, host, port);
            System.out.println("session created.");

            // disabling StrictHostKeyChecking may help to make connection but makes it insecure
            // see http://stackoverflow.com/questions/30178936/jsch-sftp-security-with-session-setconfigstricthostkeychecking-no
            java.util.Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);

            session.connect();
            System.out.println("session connected.....");

            Channel channel = session.openChannel("sftp");
            channel.setInputStream(System.in);
            channel.setOutputStream(System.out);
            channel.connect();
            System.out.println("shell channel connected....");

            ChannelSftp channelSftp = (ChannelSftp) channel;

            this.channelSftp = channelSftp;
        } catch (JSchException e) {
            throw new RuntimeException("SFTP session establishment failed: "+e);
        }
    }

    public String getCurrentDirectory() {
        log.info(printMethodName());

        String currentDirectory = "";
        try {
            currentDirectory=channelSftp.pwd();
            System.out.println("CurrentDirectory: "+currentDirectory);
        } catch (SftpException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to get current directory");
        }
        return currentDirectory;
    }

    public String goToDirectory(String goToDirectoryPath) {
        log.info(printMethodName());

        System.out.println("CurrentDirectory: "+getCurrentDirectory());

        String currentDirectory = "";
        try {
            System.out.println("About to navigate to directory: "+goToDirectoryPath);
            channelSftp.cd(goToDirectoryPath);

        } catch (SftpException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to change directory "+goToDirectoryPath);
        }

        String newCurrentDirectory = getCurrentDirectory();
        System.out.println("New currentDirectory: "+newCurrentDirectory);

        return newCurrentDirectory;
    }

    //public String createDirectory(String createNewDirectoryPath, String chmod) {
    public String createDirectory(String createNewDirectoryPath) {
        log.info(printMethodName());

        System.out.println("CurrentDirectory: "+getCurrentDirectory());
        String newCurrentDirectory="";
        try {
            System.out.println("About to create directory: "+createNewDirectoryPath);
            //mkdirs(createNewDirectoryPath, chmod);
            mkdirs(createNewDirectoryPath);
            newCurrentDirectory = goToDirectory(createNewDirectoryPath);
            System.out.println("New currentDirectory:"+ newCurrentDirectory);
        } catch (Throwable e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to create directory "+createNewDirectoryPath);
        }
        return newCurrentDirectory;
    }

    public boolean uploadFiles(String remoteFolder, String... localFiles) {
        log.info(printMethodName());

        for (String file:localFiles) {
            try {
                channelSftp.put(file, remoteFolder);
            } catch (SftpException e) {
                e.printStackTrace();
                throw new RuntimeException("Failed to upload file "+file+ " to " + remoteFolder);
            }
        }

        System.out.println("Successfully uploaded following files to "+ remoteFolder + ": ");
        Arrays.asList(localFiles).forEach(s -> System.out.println(s));

        return true;

    }

    public boolean changeFilesAccessPermissions(String chmod, String remoteFolder, String... localFiles) {
        log.info(printMethodName());

        for (String file:localFiles) {
            try {
                int chmodInt = Integer.parseInt(chmod, 8);
                channelSftp.chmod(chmodInt, remoteFolder+"/"+file);
            } catch (SftpException e) {
                e.printStackTrace();
                throw new RuntimeException("Failed to CHMOD file "+file+ " to " + remoteFolder);
            }
        }

        System.out.println("Successfully CHMOD:ed following files in "+ remoteFolder + ": ");
        Arrays.asList(localFiles).forEach(s -> System.out.println(s));

        return true;

    }

    public boolean changeFolderAccessPermissions(String chmod, String... remoteFolders) {
        log.info(printMethodName());

        for (String folder:remoteFolders) {
            try {
                int chmodInt = Integer.parseInt(chmod, 8);
                channelSftp.chmod(chmodInt, folder);
            } catch (SftpException e) {
                e.printStackTrace();
                throw new RuntimeException("Failed to CHMOD folder "+folder);
            }
        }

        System.out.println("Successfully CHMOD:ed following folders: ");
        Arrays.asList(remoteFolders).forEach(s -> System.out.println(s));

        return true;

    }



    public boolean removeFiles(String remoteFolder, String... remoteFiles)  {
        log.info(printMethodName());

        for (String file : remoteFiles) {
            try {
                if (channelSftp.ls(remoteFolder + "/" + file).size() > 0) {
                    channelSftp.rm(remoteFolder + "/" + file);
                } else {
                    System.out.println("No file '"+file+"' found from "+ remoteFolder);
                    return false;
                }
            } catch (SftpException e) {
                e.printStackTrace();
                throw new RuntimeException("Failed to remove file " + file + " from " + remoteFolder + "!");
            }
        }

        System.out.println("Successfully removed following files from "+ remoteFolder + ": ");
        Arrays.asList(remoteFiles).forEach(s -> System.out.println(s));

        return true;
    }

    /*Removes just folder if it is empty*/
    public boolean removeFolder(String remoteFolder) {
        log.info(printMethodName());

        boolean remoteDirExists = true;

        try {
            channelSftp.ls(remoteFolder).isEmpty();
        } catch (SftpException e) {
            //e.printStackTrace();
            System.out.println("No such directory: "+ remoteFolder);
            remoteDirExists = false;
        }

        try {
            if (remoteDirExists == true) {
                channelSftp.rmdir(remoteFolder);
                System.out.println("Successfully removed following directory "+ remoteFolder + ": ");
                return true;
            }
        } catch (SftpException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to remove directory: " + remoteFolder + "!");
        }

        return false;
    }

    /*Removes folder and subfolders including files in them recursively*/
    public boolean removeFolderRecursively(String remoteFolder) throws SftpException {
        log.info(printMethodName());

        try {
            channelSftp.ls(remoteFolder).size();
        } catch (Throwable e) {
            if (e.getMessage().contains("No such file")) {
                System.out.println("No such folder: "+ remoteFolder);
                return false;
            }
        }

        channelSftp.cd(remoteFolder); // Change Directory on SFTP Server
        // List source directory structure.
        Vector<ChannelSftp.LsEntry> fileAndFolderList = channelSftp.ls(remoteFolder);

        // Iterate objects in the list to get file/folder names.
        for (ChannelSftp.LsEntry item : fileAndFolderList) {

            String fn = item.getFilename();
            // If it is a file (not a directory).
            if (!item.getAttrs().isDir()) {
                channelSftp.rm(remoteFolder + "/" + item.getFilename()); // Remove file.

            } else if (!(".".equals(item.getFilename()) || "..".equals(item.getFilename()))) { // If it is a subdir.
                try {
                    // removing sub directory.
                    channelSftp.rmdir(remoteFolder + "/" + item.getFilename());
                } catch (Exception e) { // If subdir is not empty and error occurs.
                    // Do lsFolderRemove on this subdir to enter it and clear its contents.
                    removeFolderRecursively(remoteFolder + "/" + item.getFilename());
                }
            }
        }
        channelSftp.rmdir(remoteFolder); // delete the parent directory after empty
        System.out.println("Successfully removed following directory: "+ remoteFolder);

        return true;
    }

    public void exit() {
        channelSftp.exit();
    }

    private void mkdirs(String path) {
        try {
            String[] folders = path.split("/");
            if (folders[0].isEmpty()) folders[0] = "/";
            String fullPath = folders[0];
            for (int i = 1; i < folders.length; i++) {
                Vector ls = channelSftp.ls(fullPath);
                boolean isExist = false;
                for (Object o : ls) {
                    if (o instanceof ChannelSftp.LsEntry) {
                        ChannelSftp.LsEntry e = (ChannelSftp.LsEntry) o;
                        if (e.getAttrs().isDir() && e.getFilename().equals(folders[i])) {
                            isExist = true;
                        }
                    }
                }
                if (!isExist && !folders[i].isEmpty()) {
                    channelSftp.mkdir(fullPath + folders[i]);
                }
                fullPath = fullPath + folders[i] + "/";
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}