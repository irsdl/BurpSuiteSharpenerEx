# Burp Suite Sharpener
This extension enhances Burp Suite by adding several UI and functional features, making it more user-friendly. 

Refer to the "Burp Suite Compatibility" sections for detailed information on compatibility.

<pre>
 ___  _                                      
/ __>| |_  ___  _ _  ___  ___ ._ _  ___  _ _ 
\__ \| . |<_> || '_>| . \/ ._>| ' |/ ._>| '_>
<___/|_|_|<___||_|  |  _/\___.|_|_|\___.|_|
                    |_|
</pre>

![src/main/resources/sharpener.png](src/main/resources/sharpener.png)

# Installation
* Download the latest jar file built by GitHub from [/releases/latest](https://github.com/irsdl/BurpSuiteSharpenerEx/releases/latest), or by going through the [Workflows' Artifacts](https://github.com/irsdl/BurpSuiteSharpenerEx/actions).
* Add it to Burp Suite using the `Extensions` tab

# Features
* Making main tools' tabs more distinguishable by choosing a theme
* Ability to control style of sub-tabs in Repeater and Intruder
* Ability to change Burp Suite title and its icon
* Copy & pasting style ability for Repeater and Intruder tabs
* Pasting style for Repeater and Intruder tabs when their title matches a Regular Expression
* Copy & pasting titles by creating unique titles by adding a number in the end
* Rename titles without a need to double-click on the title
* Jump to first and last tabs in Repeater and Intruder
* Back and Forward feature depends on the previously selected tabs
* Finding Repeater and Intruder tabs when their title matches a Regular Expression
* Scrollable main tool tabs
* Scrollable Repeater and Intruder tabs
* Taking screenshot of repeater or intruder tabs
* Trimming long titles into 100 characters
* Show previously chosen titles for a tab
* Several keyboard shortcuts to make the tab navigation easier
* Support for PwnFox Firefox extension highlighter
* Ability to save the last size and position of Burp Suite to move it to the same location next time
* Ability to detect off-screen Burp Suite window to bring it to the centre
* Ability to highlight HTTP and WebSocket requests in proxy by detecting the "tempcolorCOLORNAME" pattern and removing it
* Ability to highlight HTTP and WebSocket requests and responses in proxy by detecting the "permcolorCOLORNAME" pattern

# Burp Suite Compatibility and Reporting Errors
This extension was last tested with the pro version 2023.10.1.1, the most recent stable release at the time of this documentation. It should also be compatible with the community edition.

For those keen on using the latest updates, the early-adapter branch is available.

We actively use this extension and occasionally observe potential errors, particularly when Burp Suite updates its core functionalities or UI. As an open-source project, we heavily rely on community feedback for enhancements and bug fixes. If you encounter any issues, kindly report them on our [issues page](https://github.com/irsdl/BurpSuiteSharpenerEx/issues). Additionally, if you value our work, please consider [sponsoring](https://github.com/sponsors/irsdl) this project.

# Using the Legacy Extension
In the latest version of this extension, only the most recent version of Burp Suite is supported, as noted above. For older versions of Burp Suite, you can use the legacy version of the extension or refer to the original repository.
The older versions of this extension can be downloaded from the legacy branch (with no support):
https://github.com/mdsecresearch/BurpSuiteSharpener/tree/Legacy-Extension/release
or
https://github.com/mdsecresearch/BurpSuiteSharpener/releases

# About this repository
Continuation of the Burp Suite Sharpener project originally from https://github.com/mdsecresearch/BurpSuiteSharpener.

# Suggesting New Features
The plan is to add simple but effective missing features to this single extension to make tester's life easier as a must-have companion when using Burp Suite (so we cannot Burp without it!).

Please feel free to submit your new feature requests using `FR: ` in its title in [issues](https://github.com/irsdl/BurpSuiteSharpenerEx/issues).

It would be great to also list any known available extensions which might have implemented suggested features. 
Perhaps the best features can be imported from different open-source extensions so the overhead of adding different extensions can be reduced.
  
# Usage Tips
* You can use the following key combination(s) in Repeater and Intruder sub-tab menu:

| Description                                      | Combinations/Shortcuts                                                                             |
|--------------------------------------------------|----------------------------------------------------------------------------------------------------|
| Show Context Menu for Repeater and Intruder Tabs | Mouse Middle-Click<br/>Alt + Any Mouse Key<br/>Down Arrow<br/>Ctrl + Enter<br/>Ctrl + Shift +Enter |
| Find Tabs for Repeater and Intruder Tab          | Ctrl + Shift + F                                                                                   |
| Find Next                                        | F3 <br/>Ctrl + F3                                                                                  |
| Find Previous                                    | Shift + F3<br/>Ctrl + Shift + F3                                                                   |
| Jump to the First Tab                            | Home<br/>Ctrl + Shift + Home                                                                       |
| Jump to the last Tab                             | End<br/>Ctrl + Shift + End                                                                         |
| Previous Tab                                     | Left Arrow<br/>Ctrl + Shift + Left                                                                 |
| Next Tab                                         | Right Arrow<br/>Ctrl + Shift + Right<br/>Mouse Wheel                                               |
| Back (Previously Selected Tab)                   | Alt + Left<br/>Ctrl + Alt + Left<br/>Mouse Wheel                                                   |
| Forward                                          | Alt + Right<br/>Ctrl + Alt + Right                                                                 |
| Copy Subtab Title                                | Ctrl + C<br/>Ctrl + Shift + C                                                                      |
| Paste Subtab Title                               | Ctrl + V<br/>Ctrl + Shift + V                                                                      |
| Rename Subtab Title                              | F2<br/>Ctrl + F2                                                                                   |
| Increase Font Size                               | Ctrl + Mouse Wheel                                                                                 |
| Increase Font Size & Bold                        | Middle Click + CTRL                                                                                |
| Decrease Font Size & Bold                        | Middle Click + CTRL + SHIFT                                                                        |
| Big & Red & Bold                                 | Middle Click + SHIFT                                                                               |

* You can use the following key combination(s) on the main window frame:

| Description                                        | Combinations/Shortcuts |
|----------------------------------------------------|------------------------|
| Move Burp Suite Window to the centre of the Screen | Ctrl + Alt + C         |

* After setting style on a sub-tab, setting the same title on another sub-tab will copy its style
* Alt + Any Mouse Click works on empty parts of the tabs which do not contain any text
* Use the `Debug` option in `Global Settings` if you are reporting a bug or if you want to see what is happening
* Check the [extension's GitHub repository](https://github.com/irsdl/BurpSuiteSharpenerEx) rather than BApp Store for the latest updates
* A sample of icons should also be accessible in the `/` directory

![images/img.png](images/img.png)

![images/img_0.png](images/img_0.png)

![images/img_1.png](images/img_1.png)

![images/img_2.png](images/img_2.png)

![images/img_3.png](images/img_3.png)

![images/img_4.png](images/img_4.png)

![images/img_5.png](images/img_5.png)

![images/img_6.png](images/img_6.png)

![images/img_7.png](images/img_7.png)

![images/img_8.png](images/img_8.png)

![images/img_9.png](images/img_9.png)

# Thanks To
* Corey Arthur [CoreyD97](https://twitter.com/CoreyD97) for https://github.com/CoreyD97/Burp-Montoya-Utilities/
* Bruno Demarche (for initial Swing hack inspiration)

Please feel free to report bugs, suggest features, or send pull requests.
