package microservice.pages;

import com.codeborne.selenide.Condition;
import microservice.common.MsCommon;
import microservice.helper.SeleniumHelper;
import org.openqa.selenium.By;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.page;
import static microservice.helper.SeleniumHelper.printMethodName;

public class AddProductPage {


    public AddProductPage addCatalogItem(String catalogItemName, String catalogItemPrice) {
        printMethodName();

        $(By.id("name")).shouldBe(Condition.visible).val(catalogItemName);
        $(By.id("price")).shouldBe(Condition.visible).val(catalogItemPrice);
        $(By.cssSelector("#saveItem")).shouldBe(Condition.visible).shouldBe(Condition.enabled).click();
        $(By.xpath("//h1[contains(text(),'Success')]")).shouldBe(Condition.visible);

        System.out.println("Successfully added catalog item: "+catalogItemName);

        return page(AddProductPage.class);
    }

    public ProductsPage navigateBackToProductsPage() {
        printMethodName();

        MsCommon.navigateBackToProductsPage();
        SeleniumHelper.myDontHurryTooMuch();

        return page(ProductsPage.class);
    }
}
