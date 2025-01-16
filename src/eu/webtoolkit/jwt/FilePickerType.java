/*
 * Copyright (C) 2020 Emweb bv, Herent, Belgium.
 *
 * See the LICENSE file for terms of use.
 */
package eu.webtoolkit.jwt;

import eu.webtoolkit.jwt.auth.*;
import eu.webtoolkit.jwt.auth.mfa.*;
import eu.webtoolkit.jwt.chart.*;
import eu.webtoolkit.jwt.servlet.*;
import eu.webtoolkit.jwt.utils.*;
import java.io.*;
import java.lang.ref.*;
import java.time.*;
import java.util.*;
import java.util.regex.*;
import javax.servlet.*;
import javax.servlet.http.*;

/**
 * Enumeration of file picker types.
 *
 * <p>The browser can open the file picker in two modes: one where only files can be selected and
 * another where only directories can be selected.
 */
public enum FilePickerType {
  /** No file picker. */
  None,
  /** Only files can be selected. */
  FileSelection,
  /** Only directories can be selected. */
  DirectorySelection;

  /** Returns the numerical representation of this enum. */
  public int getValue() {
    return ordinal();
  }
}
