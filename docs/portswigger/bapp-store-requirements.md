# BApp Store acceptance criteria

You can share your extensions with the community by submitting them to the BApp Store. We review all submitted BApps for security and quality, before we make a decision on whether to include them in the BApp Store.

Before you submit your extension, make sure that it meets the following acceptance criteria:

1. #### It performs a unique function.

   Make sure that your extension doesn't duplicate the function of an existing extension in the BApp Store.

   If your idea isn't entirely new, you might be better off tailoring an existing BApp to suit your purposes. You can find the source code for every extension in the BApp Store on our [GitHub repository](https://github.com/PortSwigger).

2. #### It has a clear, descriptive name.

   Make sure that the name clearly describes what the extension does.

   You can also provide a one-line summary that appears in the list (web only), as well as a more detailed description.

3. #### It operates securely.

   Users may be testing sites that they don't trust, so it's important that extensions don't expose users to attack. Treat the content of HTTP messages as untrusted. Extensions should operate securely in expected usage. Data entered by a user into the GUI can generally be trusted, but if there is auto-fill from untrusted sources, don't assume the user will check the contents.

4. #### It includes all dependencies.

   A major benefit of the BApp Store is one-click installation. If your extension includes all dependencies, it is much easier for users to get started. This also avoids version mismatches, where an underlying tool is updated but the BApp is not.

5. #### It uses threads to maintain responsiveness.

   To maintain responsiveness, perform slow operations in a background thread:

   - Don't perform slow operations - such as HTTP requests - in the Swing Event Dispatch Thread. This causes Burp to appear unresponsive, as the whole GUI must wait until the slow operation completes.
   - Avoid slow operations when using `ProxyHttpRequestHandler`, `ProxyHttpResponseHandler` and `HttpHandler`.
   - To avoid concurrency issues, protect shared data structures with locks, and take care to avoid deadlocks.

   > #### Note
   > 
   > Burp does not catch and report exceptions in background threads. To report background exceptions, surround the full thread operation with a try/catch block and write any stack traces to the extension error stream.

6. #### It unloads cleanly.

   When an extension unloads, make sure that it releases all resources. The extension needs to register an unload handler, via `Extension.registerUnloadingHandler()`. The most common example of resources to be unloaded is background threads; it's important that background threads are terminated in `ExtensionUnloadingHandler.extensionUnloaded()`.

7. #### It uses Burp networking.

   When making an HTTP request - to the target, or otherwise - it's preferable to use Burp's `Http.issueHttpRequest()`, instead of libraries like `java.net.URL`. This sends the request through the Burp core, so settings like upstream proxies and session handling rules will be obeyed. Many users are on a corporate network that only allows Internet access through a proxy. In addition, avoid performing any communication to the target from within `ScanCheck.passiveAudit()`.

8. #### It supports offline working.

   Some Burp users need to operate from high-security networks without Internet access. To support these users, extensions that contact an online service to receive vulnerability definitions or other data should include a copy of recent definitions.

9. #### It can cope with large projects.

   Some users work with very large projects. To support such users, avoid keeping long-term references to objects passed to functions like `HttpHandler.handleHttpRequest()` or `ScanCheck.activeAudit()`. If you need to keep a long-term reference to an HTTP message, use `Persistence.temporaryFileContext()`. Also, take care with `SiteMap.requestResponses()` and `Proxy.history()` as these can return huge results.

10. #### It provides a parent for GUI elements.

    Make sure that any GUI elements that the extension creates, such as popup windows or messages, are children of the main Burp Frame. This is particularly important when users have multiple monitors, so that popups appear on the correct one.

    To get the Burp Frame, use `SwingUtils.suiteFrame()`.

11. #### It uses the Montoya API artifact.

    You should reference the `montoya-api` artifact using a build tool like Gradle or Maven. If you're starting a new project, we recommend using Gradle. For more information, see [Creating Burp extensions](/burp/documentation/desktop/extend-burp/extensions/creating).

12. #### It uses the Montoya API for AI functionality

    Extensions implementing AI functionality must use the dedicated Montoya API methods and follow the best practices outlined in [AI extension best practices](/burp/documentation/desktop/extend-burp/extensions/creating/creating-ai-extensions/best-practices). This ensures ease of setup for users, as they won't need to configure third-party API keys or services. It also enhances security, as all AI-related traffic is handled through Burp Suite's dedicated AI platform.