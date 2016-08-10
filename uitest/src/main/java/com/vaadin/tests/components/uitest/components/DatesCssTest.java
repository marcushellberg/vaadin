package com.vaadin.tests.components.uitest.components;

import java.util.Calendar;
import java.util.GregorianCalendar;

import com.vaadin.legacy.ui.LegacyDateField;
import com.vaadin.legacy.ui.LegacyInlineDateField;
import com.vaadin.legacy.ui.LegacyPopupDateField;
import com.vaadin.tests.components.uitest.TestSampler;
import com.vaadin.ui.Component;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.themes.ChameleonTheme;

public class DatesCssTest extends GridLayout {

    private TestSampler parent;
    private int debugIdCounter = 0;

    private Calendar cal = new GregorianCalendar(2012, 8, 11, 18, 00, 00);

    public DatesCssTest(TestSampler parent) {
        super(5, 2);
        this.parent = parent;
        setSpacing(true);
        setWidth("100%");

        createDateFieldWith(null, null, null);
        createDateFieldWith("Small", ChameleonTheme.DATEFIELD_SMALL, null);
        createDateFieldWith("Big", ChameleonTheme.DATEFIELD_BIG, null);

        LegacyDateField df = new LegacyPopupDateField("Popup date field");
        df.setId("datefield" + debugIdCounter++);
        df.setValue(cal.getTime());
        addComponent(df);

        df = new LegacyInlineDateField("Inline date field");
        df.setId("datefield" + debugIdCounter++);
        df.setValue(cal.getTime());
        addComponent(df);

        createDateFieldWith(null, null, "130px");
        createDateFieldWith("Small 130px", ChameleonTheme.DATEFIELD_SMALL,
                "130px");
        createDateFieldWith("Big 130px", ChameleonTheme.DATEFIELD_BIG, "130px");

    }

    private void createDateFieldWith(String caption, String primaryStyleName,
            String width) {
        LegacyDateField df = new LegacyDateField("Date field");
        df.setId("datefield" + debugIdCounter++);
        df.setValue(cal.getTime());

        if (caption != null) {
            df.setCaption(caption);
        }

        if (primaryStyleName != null) {
            df.addStyleName(primaryStyleName);
        }
        if (width != null) {
            df.setWidth(width);
        }

        addComponent(df);

    }

    @Override
    public void addComponent(Component c) {
        parent.registerComponent(c);
        super.addComponent(c);
    }

}
