// Vendored from Burp-Montoya-Utilities by Corey Arthur (@CoreyD97)
// https://github.com/CoreyD97/Burp-Montoya-Utilities (commit b7faf563)
// Copyright (C) Corey Arthur
// Released under AGPL v3.0, see the LICENSE file; not covered by the
// additional terms in the NOTICE file
// Modifications (repackaging) Copyright (C) 2026 Soroush Dalili (@irsdl)

package ninja.burpsuite.libs.thirdparty.burpextenderutilities.nameManager;

import java.util.HashSet;
import java.util.Set;

public class NameManager{
  public static void reserve(String newName){
    if(!_nameSet.add(newName))
      throw new NameCollisionException("Name " + newName + " is already reserved.");
  }

  public static void release(String name){
    //this might not actually need to throw an exception... it may not be useful... not sure
    if(!_nameSet.remove(name))
      throw new KeyNotReservedException("Name " + name + " was not previously reserved.");
  }

  public static boolean isReserved(String name){ return _nameSet.contains(name); }

  private static final Set<String> _nameSet = new HashSet<>();
}
