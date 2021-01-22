macOs TrayIcon Fixer
===============

Fix appearance of Java TrayIcon on macOs Mojave, Catalina and BigSur.

Why use it?
-----
* Identify current theme taskbar.
* Support dynamic taskbar theme change on BigSur.

Usage
-----

```java
// Verify theme to choose image version
Image iconImage = MacOSTrayIconFixer.getInitialIcon(blackImage, whiteImage);
// Create your TrayIcon using image
TrayIcon icon = new TrayIcon(iconImage, "Test");
// (Optional) add action and menu
icon.addActionListener((e) -> System.out.println("examples.BasicUsage.main():" + SwingUtilities.isEventDispatchThread()));

// Include in SystemTray BEFORE call fixer
SystemTray.getSystemTray().add(icon);

// Fix
MacOSTrayIconFixer.fix(icon, blackImage, whiteImage, false, 0);
```