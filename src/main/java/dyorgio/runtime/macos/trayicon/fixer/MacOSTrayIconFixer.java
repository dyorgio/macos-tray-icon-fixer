/*
 * The MIT License
 *
 * Copyright 2020 dyorgio.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package dyorgio.runtime.macos.trayicon.fixer;

import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import dyorgio.runtime.macos.trayicon.fixer.jna.foundation.ActionCallback;
import dyorgio.runtime.macos.trayicon.fixer.jna.foundation.Foundation;
import dyorgio.runtime.macos.trayicon.fixer.jna.foundation.FoundationUtil;
import dyorgio.runtime.macos.trayicon.fixer.jna.foundation.NSDictionary;
import dyorgio.runtime.macos.trayicon.fixer.jna.foundation.NSString;
import dyorgio.runtime.macos.trayicon.fixer.jna.foundation.NSUserDefaults;
import java.awt.Image;
import java.awt.MenuComponent;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Field;
import java.util.Arrays;
import javax.swing.SwingUtilities;

/**
 *
 * @author dyorgio
 */
public final class MacOSTrayIconFixer {

    private static final String OS_VERSION = new NSString(NSDictionary.dictionaryWithContentsOfFile(new NSString("/System/Library/CoreServices/SystemVersion.plist"))//
            .objectForKey(new NSString("ProductVersion")).getId()).toString();

    MacOSTrayIconFixer() {
    }

    public static Image getInitialIcon(Image blackImage, Image whiteImage) {
        return !isImageTemplateSupported() && isDarkTheme() ? whiteImage : blackImage;
    }

    public static void fix(TrayIcon icon, Image blackImage, Image whiteImage) {
        fix(icon, blackImage, whiteImage, true, 0);
    }

    public static void fix(final TrayIcon icon, Image blackImage, Image whiteImage, boolean needsMenu, int lenght) {

        // Check if icon has a menu
        if (needsMenu && icon.getPopupMenu() == null) {
            throw new IllegalStateException("PopupMenu needs to be setted on TrayIcon before fix it.");
        }
        // Check if icon is on systemtray
        if (!Arrays.asList(SystemTray.getSystemTray().getTrayIcons()).contains(icon)) {
            throw new IllegalStateException("TrayIcon needs to be added on SystemTray before fix it.");
        }

        try {
            Field ptrField = Class.forName("sun.lwawt.macosx.CFRetainedResource").getDeclaredField("ptr");
            ptrField.setAccessible(true);

            Field field = TrayIcon.class.getDeclaredField("peer");
            field.setAccessible(true);
            long cTrayIconAddress = ptrField.getLong(field.get(icon));

            long cPopupMenuAddressTmp = -1;
            if (needsMenu || icon.getPopupMenu() != null) {
                field = MenuComponent.class.getDeclaredField("peer");
                field.setAccessible(true);
                cPopupMenuAddressTmp = ptrField.getLong(field.get(icon.getPopupMenu()));
            }

            final long cPopupMenuAddress = cPopupMenuAddressTmp;

            final NativeLong statusItem = FoundationUtil.invoke(new NativeLong(cTrayIconAddress), "theItem");
            NativeLong view = FoundationUtil.invoke(statusItem, "view");
            final NativeLong image = Foundation.INSTANCE.object_getIvar(view, Foundation.INSTANCE.class_getInstanceVariable(FoundationUtil.invoke(view, "class"), "image"));
            FoundationUtil.runOnMainThreadAndWait(new Runnable() {
                @Override
                public void run() {
                    FoundationUtil.invoke(statusItem, "setView:", (Object) null);
                    Pointer buttonSelector = Foundation.INSTANCE.sel_registerName("button");
                    FoundationUtil.invoke(statusItem, buttonSelector, (Object) null);
                    FoundationUtil.invoke(image, "setTemplate:", true);
                    NativeLong button = FoundationUtil.invoke(statusItem, buttonSelector);
                    FoundationUtil.invoke(button, "setImage:", image);
                    FoundationUtil.invoke(statusItem, "setLength:", -2d);
                    if (cPopupMenuAddress > 0) {
                        FoundationUtil.invoke(statusItem, "setMenu:", FoundationUtil.invoke(new NativeLong(cPopupMenuAddress), "menu"));
                    }
                    new ActionCallback(new Runnable() {
                        @Override
                        public void run() {
                            final ActionListener[] listeners = icon.getActionListeners();
                            final int now = (int) System.currentTimeMillis();
                            for (int i = 0; i < listeners.length; i++) {
                                final int iF = i;
                                SwingUtilities.invokeLater(new Runnable() {
                                    @Override
                                    public void run() {
                                        listeners[iF].actionPerformed(new ActionEvent(icon, now + iF, null));
                                    }
                                });
                            }
                        }
                    }).installActionOnNSControl(button);
                }
            });
        } catch (Throwable t) {
            // ignore all
        }
    }

    public static boolean isImageTemplateSupported() {
        return OS_VERSION.compareTo("10.5") >= 0;
    }

    public static boolean isDarkTheme() {
        return "Dark".equals(NSUserDefaults.standard().stringForKey(new NSString("AppleInterfaceStyle")).toString());
    }
}
