package com.vaadin.tests.components.datefield;

import java.util.Date;
import java.util.Locale;

import com.vaadin.legacy.ui.LegacyDateField;
import com.vaadin.tests.components.TestBase;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;

public class DateFieldLocale extends TestBase {

    @Override
    public void setup() {
        final LegacyDateField dateField = new LegacyDateField("DateField");
        dateField.setLocale(new Locale("fi", "FI"));
        dateField.setCaption(dateField.getLocale().toString());
        dateField.setValue(new Date(2013 - 1900, 7 - 1, 27));
        dateField.setResolution(LegacyDateField.RESOLUTION_DAY);

        addComponent(new Button("Change locale", new ClickListener() {

            @Override
            public void buttonClick(ClickEvent event) {
                if (dateField.getLocale().getCountry().equalsIgnoreCase("fi")) {
                    dateField.setLocale(new Locale("zh", "CN"));
                } else {
                    dateField.setLocale(new Locale("fi", "FI"));
                }
                dateField.setCaption(dateField.getLocale().toString());
            }
        }));

        addComponent(dateField);
    }

    @Override
    protected String getDescription() {
        return "Click change locale to switch between Finnish and Chinese locale for the DateField. The date string should be updated in addition to the caption.";
    }

    @Override
    protected Integer getTicketNumber() {
        return 3935;
    }

}
