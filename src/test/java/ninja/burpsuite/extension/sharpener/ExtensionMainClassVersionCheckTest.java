// Copyright (C) 2021-2026 Soroush Dalili (@irsdl)
// Released under AGPL v3.0 with additional terms, see the LICENSE and NOTICE files
// Developed by Soroush Dalili (@irsdl)

package ninja.burpsuite.extension.sharpener;

import burp.api.montoya.http.Http;
import burp.api.montoya.http.RequestOptions;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

// The update check downloads a version file and can send the user to a download
// page, so the request must verify the upstream TLS certificate. Burp skips that
// verification unless withUpstreamTLSVerification() is set on the request options.
public class ExtensionMainClassVersionCheckTest {

    @Test
    void versionCheckRequestEnablesUpstreamTlsVerification() {
        Http http = mock(Http.class);
        HttpRequest request = mock(HttpRequest.class);
        RequestOptions baseOptions = mock(RequestOptions.class);
        RequestOptions verifiedOptions = mock(RequestOptions.class);
        HttpRequestResponse response = mock(HttpRequestResponse.class);
        when(baseOptions.withUpstreamTLSVerification()).thenReturn(verifiedOptions);
        when(http.sendRequest(request, verifiedOptions)).thenReturn(response);

        assertSame(response, ExtensionMainClass.sendVersionCheckRequest(http, request, baseOptions));

        verify(baseOptions).withUpstreamTLSVerification();
        verify(http).sendRequest(request, verifiedOptions);
        // the single argument overload would silently skip certificate verification
        verify(http, never()).sendRequest(any(HttpRequest.class));
    }
}
