// Vendored from Burp-Montoya-Utilities by Corey Arthur (@CoreyD97)
// https://github.com/CoreyD97/Burp-Montoya-Utilities (commit b7faf563)
// Copyright (C) Corey Arthur
// Released under AGPL v3.0, see the LICENSE file; not covered by the
// additional terms in the NOTICE file
// Modifications (repackaging) Copyright (C) 2026 Soroush Dalili (@irsdl)

package ninja.burpsuite.libs.thirdparty.burpextenderutilities;

public class UnmanagedSettingException extends RuntimeException{
  public UnmanagedSettingException(){}

  public UnmanagedSettingException(String message){ super(message); }

  public UnmanagedSettingException(String message, Throwable cause){
    super(message, cause);
  }

  public UnmanagedSettingException(
    String message, Throwable cause,
    boolean enableSuppression, boolean writableStackTrace
  ){
    super(message, cause, enableSuppression, writableStackTrace);
  }

  public UnmanagedSettingException(Throwable cause){ super(cause); }
}
