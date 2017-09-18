/*
 * Copyright 2017 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package utils;

import java.nio.charset.Charset;
import java.util.Objects;

/**
 * @author galderz
 */
public class DomainUtils {

  private static final Charset CHARSET = Charset.forName("UTF-8");

  public static byte[] bs(String s) {
    return Objects.isNull(s) ? null : s.getBytes(CHARSET);
  }

  public static String str(byte[] bs) {
    return Objects.isNull(bs) ? null : new String(bs, CHARSET);
  }

}
