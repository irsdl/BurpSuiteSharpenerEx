// Burp Suite Extension Name: Sharpener
// Copyright (C) 2021-2026 Soroush Dalili (@irsdl)
// Released under AGPL v3.0 with additional terms, see the LICENSE and NOTICE files
// Developed by Soroush Dalili (@irsdl)
// Project link: https://github.com/irsdl/BurpSuiteSharpenerEx

package ninja.burpsuite.extension.sharpener.capabilities.objects;

public enum CapabilityGroup {
    AUDIT_ISSUE_HANDLER,
    CONTEXT_MENU_ITEMS_PROVIDER,
    HTTP_HANDLER,
    HTTP_REQUEST_EDITOR_PROVIDER,
    HTTP_RESPONSE_EDITOR_PROVIDER,
    INSERTION_POINT_PROVIDER,
    MESSAGE_HANDLER,
    PAYLOAD_GENERATOR_PROVIDER,
    PAYLOAD_PROCESSOR,
    PROXY_MESSAGE_HANDLER,
    PROXY_REQUEST_HANDLER,
    PROXY_RESPONSE_HANDLER,
    SCAN_CHECK,
    SCOPE_CHANGE_HANDLER,
    SESSION_HANDLING_ACTION,
    UNLOADING_HANDLER,
    WEBSOCKET_CREATION_HANDLER,
    WEBSOCKET_MESSAGE_EDITOR_PROVIDER,
    OTHER
}
