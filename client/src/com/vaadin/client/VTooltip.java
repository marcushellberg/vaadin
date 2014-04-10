/*
 * Copyright 2000-2013 Vaadin Ltd.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.client;

import com.google.gwt.aria.client.LiveValue;
import com.google.gwt.aria.client.RelevantValue;
import com.google.gwt.aria.client.Roles;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.DomEvent;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.dom.client.MouseMoveHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;
import com.vaadin.client.ui.VWindowOverlay;

/**
 * TODO open for extension
 */
public class VTooltip extends VWindowOverlay {
    private static final String CLASSNAME = "v-tooltip";
    private static final int MARGIN = 4;
    public static final int TOOLTIP_EVENTS = Event.ONKEYDOWN
            | Event.ONMOUSEOVER | Event.ONMOUSEOUT | Event.ONMOUSEMOVE
            | Event.ONCLICK;
    VErrorMessage em = new VErrorMessage();
    Element description = DOM.createDiv();

    private boolean closing = false;
    private boolean opening = false;

    // Open next tooltip faster. Disabled after 2 sec of showTooltip-silence.
    private boolean justClosed = false;

    private String uniqueId = DOM.createUniqueId();
    private int maxWidth;

    // Delays for the tooltip, configurable on the server side
    private int openDelay;
    private int quickOpenDelay;
    private int quickOpenTimeout;
    private int closeTimeout;

    /**
     * Used to show tooltips; usually used via the singleton in
     * {@link ApplicationConnection}. NOTE that #setOwner(Widget)} should be
     * called after instantiating.
     * 
     * @see ApplicationConnection#getVTooltip()
     */
    public VTooltip() {
        super(false, false, true);
        setStyleName(CLASSNAME);
        FlowPanel layout = new FlowPanel();
        setWidget(layout);
        layout.add(em);
        DOM.setElementProperty(description, "className", CLASSNAME + "-text");
        DOM.appendChild(layout.getElement(), description);
        setSinkShadowEvents(true);

        // When a tooltip is shown, the content of the tooltip changes. With a
        // tooltip being a live-area, this change is notified to a assistive
        // device.
        Roles.getTooltipRole().set(getElement());
        Roles.getTooltipRole().setAriaLiveProperty(getElement(),
                LiveValue.ASSERTIVE);
        Roles.getTooltipRole().setAriaRelevantProperty(getElement(),
                RelevantValue.ADDITIONS);
    }

    /**
     * Show the tooltip with the provided info for assistive devices.
     * 
     * @param info
     *            with the content of the tooltip
     */
    public void showAssistive(TooltipInfo info) {
        updatePosition(null, true);
        show(info);
    }

    /**
     * Show a popup containing the information in the "info" tooltip
     * 
     * @param info
     */
    private void show(TooltipInfo info) {
        boolean hasContent = false;
        if (info.getErrorMessage() != null) {
            em.setVisible(true);
            em.updateMessage(info.getErrorMessage());
            hasContent = true;
        } else {
            em.setVisible(false);
        }
        if (info.getTitle() != null && !"".equals(info.getTitle())) {
            DOM.setInnerHTML(description, info.getTitle());
            DOM.setStyleAttribute(description, "display", "");
            hasContent = true;
        } else {
            DOM.setInnerHTML(description, "");
            DOM.setStyleAttribute(description, "display", "none");
        }
        if (hasContent) {
            // Issue #8454: With IE7 the tooltips size is calculated based on
            // the last tooltip's position, causing problems if the last one was
            // in the right or bottom edge. For this reason the tooltip is moved
            // first to 0,0 position so that the calculation goes correctly.
            setPopupPosition(0, 0);
            setPopupPositionAndShow(new PositionCallback() {
                @Override
                public void setPosition(int offsetWidth, int offsetHeight) {

                    if (offsetWidth > getMaxWidth()) {
                        setWidth(getMaxWidth() + "px");

                        // Check new height and width with reflowed content
                        offsetWidth = getOffsetWidth();
                        offsetHeight = getOffsetHeight();
                    }

                    int x = tooltipEventMouseX + 10 + Window.getScrollLeft();
                    int y = tooltipEventMouseY + 10 + Window.getScrollTop();

                    if (x + offsetWidth + MARGIN - Window.getScrollLeft() > Window
                            .getClientWidth()) {
                        x = Window.getClientWidth() - offsetWidth - MARGIN
                                + Window.getScrollLeft();
                    }

                    if (y + offsetHeight + MARGIN - Window.getScrollTop() > Window
                            .getClientHeight()) {
                        y = tooltipEventMouseY - 5 - offsetHeight
                                + Window.getScrollTop();
                        if (y - Window.getScrollTop() < 0) {
                            // tooltip does not fit on top of the mouse either,
                            // put it at the top of the screen
                            y = Window.getScrollTop();
                        }
                    }

                    setPopupPosition(x, y);
                    sinkEvents(Event.ONMOUSEOVER | Event.ONMOUSEOUT);
                }
            });
        } else {
            hide();
        }
    }

    private void showTooltip() {

        // Close current tooltip
        if (isShowing()) {
            closeNow();
        }

        // Schedule timer for showing the tooltip according to if it was
        // recently closed or not.
        int timeout = justClosed ? getQuickOpenDelay() : getOpenDelay();
        showTimer.schedule(timeout);
        opening = true;
    }

    private void closeNow() {
        hide();
        setWidth("");
        closing = false;
    }

    private Timer showTimer = new Timer() {
        @Override
        public void run() {
            TooltipInfo info = tooltipEventHandler.getTooltipInfo();
            if (null != info) {
                show(info);
            }
            opening = false;
        }
    };

    private Timer closeTimer = new Timer() {
        @Override
        public void run() {
            closeNow();
            justClosedTimer.schedule(2000);
            justClosed = true;
        }
    };

    private Timer justClosedTimer = new Timer() {
        @Override
        public void run() {
            justClosed = false;
        }
    };

    public void hideTooltip() {
        if (opening) {
            showTimer.cancel();
            opening = false;
        }
        if (!isAttached()) {
            return;
        }
        if (closing) {
            // already about to close
            return;
        }
        closeTimer.schedule(getCloseTimeout());
        closing = true;
        justClosed = true;
        justClosedTimer.schedule(getQuickOpenTimeout());
    }

    @Override
    public void hide() {
        em.updateMessage("");
        description.setInnerHTML("");

        updatePosition(null, true);
        setPopupPosition(tooltipEventMouseX, tooltipEventMouseY);
    }

    private int tooltipEventMouseX;
    private int tooltipEventMouseY;

    public void updatePosition(Event event, boolean isFocused) {
        if (isFocused) {
            tooltipEventMouseX = -1000;
            tooltipEventMouseY = -1000;
        } else {
            tooltipEventMouseX = DOM.eventGetClientX(event);
            tooltipEventMouseY = DOM.eventGetClientY(event);
        }
    }

    @Override
    public void onBrowserEvent(Event event) {
        final int type = DOM.eventGetType(event);
        // cancel closing event if tooltip is mouseovered; the user might want
        // to scroll of cut&paste

        if (type == Event.ONMOUSEOVER) {
            // Cancel closing so tooltip stays open and user can copy paste the
            // tooltip
            closeTimer.cancel();
            closing = false;
        }
    }

    /**
     * Replace current open tooltip with new content
     */
    public void replaceCurrentTooltip() {
        if (closing) {
            closeTimer.cancel();
            closeNow();
        }

        TooltipInfo info = tooltipEventHandler.getTooltipInfo();
        if (null != info) {
            show(info);
        }
        opening = false;
    }

    private class TooltipEventHandler implements MouseMoveHandler,
            ClickHandler, KeyDownHandler, FocusHandler, BlurHandler {

        /**
         * Current element hovered
         */
        private com.google.gwt.dom.client.Element currentElement = null;

        /**
         * Marker for handling of tooltip through focus
         */
        private boolean handledByFocus;

        /**
         * Current tooltip active
         */
        private TooltipInfo currentTooltipInfo = null;

        /**
         * Get current active tooltip information
         * 
         * @return Current active tooltip information or null
         */
        public TooltipInfo getTooltipInfo() {
            return currentTooltipInfo;
        }

        /**
         * Locate connector and it's tooltip for given element
         * 
         * @param element
         *            Element used in search
         * @return true if connector and tooltip found
         */
        private boolean resolveConnector(Element element) {

            ApplicationConnection ac = getApplicationConnection();
            ComponentConnector connector = Util.getConnectorForElement(ac,
                    RootPanel.get(), element);

            // Try to find first connector with proper tooltip info
            TooltipInfo info = null;
            while (connector != null) {

                info = connector.getTooltipInfo(element);

                if (info != null && info.hasMessage()) {
                    break;
                }

                if (!(connector.getParent() instanceof ComponentConnector)) {
                    connector = null;
                    info = null;
                    break;
                }
                connector = (ComponentConnector) connector.getParent();
            }

            if (connector != null && info != null) {
                assert connector.hasTooltip() : "getTooltipInfo for "
                        + Util.getConnectorString(connector)
                        + " returned a tooltip even though hasTooltip claims there are no tooltips for the connector.";
                currentTooltipInfo = info;
                return true;
            }

            return false;
        }

        /**
         * Handle hide event
         * 
         * @param event
         *            Event causing hide
         */
        private void handleHideEvent() {
            hideTooltip();
            currentTooltipInfo = null;
        }

        @Override
        public void onMouseMove(MouseMoveEvent mme) {
            handleShowHide(mme, false);
        }

        @Override
        public void onClick(ClickEvent event) {
            handleHideEvent();
        }

        @Override
        public void onKeyDown(KeyDownEvent event) {
            handleHideEvent();
        }

        /**
         * Displays Tooltip when page is navigated with the keyboard.
         * 
         * Tooltip is not visible. This makes it possible for assistive devices
         * to recognize the tooltip.
         */
        @Override
        public void onFocus(FocusEvent fe) {
            handleShowHide(fe, true);
        }

        /**
         * Hides Tooltip when the page is navigated with the keyboard.
         * 
         * Removes the Tooltip from page to make sure assistive devices don't
         * recognize it by accident.
         */
        @Override
        public void onBlur(BlurEvent be) {
            handledByFocus = false;
            handleHideEvent();
        }

        private void handleShowHide(DomEvent domEvent, boolean isFocused) {
            Event event = Event.as(domEvent.getNativeEvent());
            Element element = Element.as(event.getEventTarget());

            // We can ignore move event if it's handled by move or over already
            if (currentElement == element && handledByFocus == true) {
                return;
            }

            boolean connectorAndTooltipFound = resolveConnector(element);
            if (!connectorAndTooltipFound) {
                if (isShowing()) {
                    handleHideEvent();
                } else {
                    currentTooltipInfo = null;
                }
            } else {
                updatePosition(event, isFocused);

                if (isShowing() && !isFocused) {
                    replaceCurrentTooltip();
                } else {
                    showTooltip();
                }
            }

            handledByFocus = isFocused;
            currentElement = element;
        }
    }

    private final TooltipEventHandler tooltipEventHandler = new TooltipEventHandler();

    /**
     * Connects DOM handlers to widget that are needed for tooltip presentation.
     * 
     * @param widget
     *            Widget which DOM handlers are connected
     */
    public void connectHandlersToWidget(Widget widget) {
        Profiler.enter("VTooltip.connectHandlersToWidget");
        widget.addDomHandler(tooltipEventHandler, MouseMoveEvent.getType());
        widget.addDomHandler(tooltipEventHandler, ClickEvent.getType());
        widget.addDomHandler(tooltipEventHandler, KeyDownEvent.getType());
        widget.addDomHandler(tooltipEventHandler, FocusEvent.getType());
        widget.addDomHandler(tooltipEventHandler, BlurEvent.getType());
        Profiler.leave("VTooltip.connectHandlersToWidget");
    }

    /**
     * Returns the unique id of the tooltip element.
     * 
     * @return String containing the unique id of the tooltip, which always has
     *         a value
     */
    public String getUniqueId() {
        return uniqueId;
    }

    @Override
    public void setPopupPositionAndShow(PositionCallback callback) {
        if (isAttached()) {
            callback.setPosition(getOffsetWidth(), getOffsetHeight());
        } else {
            super.setPopupPositionAndShow(callback);
        }
    }

    /**
     * Returns the time (in ms) the tooltip should be displayed after an event
     * that will cause it to be closed (e.g. mouse click outside the component,
     * key down).
     * 
     * @return The close timeout (in ms)
     */
    public int getCloseTimeout() {
        return closeTimeout;
    }

    /**
     * Sets the time (in ms) the tooltip should be displayed after an event that
     * will cause it to be closed (e.g. mouse click outside the component, key
     * down).
     * 
     * @param closeTimeout
     *            The close timeout (in ms)
     */
    public void setCloseTimeout(int closeTimeout) {
        this.closeTimeout = closeTimeout;
    }

    /**
     * Returns the time (in ms) during which {@link #getQuickOpenDelay()} should
     * be used instead of {@link #getOpenDelay()}. The quick open delay is used
     * when the tooltip has very recently been shown, is currently hidden but
     * about to be shown again.
     * 
     * @return The quick open timeout (in ms)
     */
    public int getQuickOpenTimeout() {
        return quickOpenTimeout;
    }

    /**
     * Sets the time (in ms) that determines when {@link #getQuickOpenDelay()}
     * should be used instead of {@link #getOpenDelay()}. The quick open delay
     * is used when the tooltip has very recently been shown, is currently
     * hidden but about to be shown again.
     * 
     * @param quickOpenTimeout
     *            The quick open timeout (in ms)
     */
    public void setQuickOpenTimeout(int quickOpenTimeout) {
        this.quickOpenTimeout = quickOpenTimeout;
    }

    /**
     * Returns the time (in ms) that should elapse before a tooltip will be
     * shown, in the situation when a tooltip has very recently been shown
     * (within {@link #getQuickOpenDelay()} ms).
     * 
     * @return The quick open delay (in ms)
     */
    public int getQuickOpenDelay() {
        return quickOpenDelay;
    }

    /**
     * Sets the time (in ms) that should elapse before a tooltip will be shown,
     * in the situation when a tooltip has very recently been shown (within
     * {@link #getQuickOpenDelay()} ms).
     * 
     * @param quickOpenDelay
     *            The quick open delay (in ms)
     */
    public void setQuickOpenDelay(int quickOpenDelay) {
        this.quickOpenDelay = quickOpenDelay;
    }

    /**
     * Returns the time (in ms) that should elapse after an event triggering
     * tooltip showing has occurred (e.g. mouse over) before the tooltip is
     * shown. If a tooltip has recently been shown, then
     * {@link #getQuickOpenDelay()} is used instead of this.
     * 
     * @return The open delay (in ms)
     */
    public int getOpenDelay() {
        return openDelay;
    }

    /**
     * Sets the time (in ms) that should elapse after an event triggering
     * tooltip showing has occurred (e.g. mouse over) before the tooltip is
     * shown. If a tooltip has recently been shown, then
     * {@link #getQuickOpenDelay()} is used instead of this.
     * 
     * @param openDelay
     *            The open delay (in ms)
     */
    public void setOpenDelay(int openDelay) {
        this.openDelay = openDelay;
    }

    /**
     * Sets the maximum width of the tooltip popup.
     * 
     * @param maxWidth
     *            The maximum width the tooltip popup (in pixels)
     */
    public void setMaxWidth(int maxWidth) {
        this.maxWidth = maxWidth;
    }

    /**
     * Returns the maximum width of the tooltip popup.
     * 
     * @return The maximum width the tooltip popup (in pixels)
     */
    public int getMaxWidth() {
        return maxWidth;
    }

}
