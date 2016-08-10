package com.vaadin.tests.components.datefield;

import java.io.IOException;

import org.junit.Test;
import org.openqa.selenium.Keys;

import com.vaadin.testbench.By;
import com.vaadin.testbench.elements.ButtonElement;
import com.vaadin.tests.legacyelements.LegacyDateFieldElement;
import com.vaadin.tests.tb3.MultiBrowserTest;

public class DateFieldReadOnlyTest extends MultiBrowserTest {

    @Test
    public void readOnlyDateFieldPopupShouldNotOpen()
            throws IOException, InterruptedException {
        openTestURL();

        compareScreen("initial");
        toggleReadOnly();

        openPopup();
        compareScreen("readwrite-popup");

        closePopup();
        toggleReadOnly();
        compareScreen("readonly");
    }

    private void closePopup() {
        findElement(By.className("v-datefield-calendarpanel"))
                .sendKeys(Keys.RETURN);
    }

    private void openPopup() {
        // waiting for openPopup() in TB4 beta1:
        // http://dev.vaadin.com/ticket/13766
        $(LegacyDateFieldElement.class).first()
                .findElement(By.tagName("button")).click();
    }

    private void toggleReadOnly() {
        $(ButtonElement.class).caption("Switch read-only").first().click();
    }
}
