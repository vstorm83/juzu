/*
 * Copyright 2013 eXo Platform SAS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package juzu.impl.fs.spi.ram;

import juzu.impl.common.Resource;
import juzu.impl.common.Timestamped;
import juzu.impl.common.Tools;
import juzu.impl.fs.spi.PathType;
import juzu.impl.fs.spi.ReadWriteFileSystem;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;

/** @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a> */
public class RAMFileSystem extends ReadWriteFileSystem<String[]> {

  /** . */
  private final RAMDir root;

  /** . */
  private final URL contextURL;

  public RAMFileSystem() {
    try {
      this.root = new RAMDir(this);
      this.contextURL = new URL("juzu", null, 0, "/", new RAMURLStreamHandler(this));
    }
    catch (MalformedURLException e) {
      AssertionError ae = new AssertionError("Unexpected exception");
      ae.initCause(e);
      throw ae;
    }
  }

  @Override
  public Class<String[]> getType() {
    return String[].class;
  }

  @Override
  public String getDescription() {
    return "ram[]";
  }

  @Override
  public String[] makePath(String[] parent, String name) throws IOException {
    String[] path = Arrays.copyOf(parent, parent.length + 1);
    path[path.length - 1] = name;
    return path;
  }

  private RAMPath get(String[] path) {
    RAMPath current = root;
    for (String name : path) {
      if (current instanceof RAMDir) {
        current = ((RAMDir)current).children.get(name);
      } else {
        return null;
      }
    }
    return current;
  }

  @Override
  public void createDir(String[] dir) throws IOException {
    long lastModified = System.currentTimeMillis();
    RAMDir current = root;
    for (String name : dir) {
      RAMPath next = current.children.get(name);
      if (next == null) {
        next = new RAMDir(current, name, lastModified);
        current.children.put(name, next);
        next.parent = current;
        current = (RAMDir)next;
      } else if (next instanceof RAMDir) {
        current = (RAMDir)next;
      } else {
        throw new IOException("A file already exist");
      }
    }
  }

  @Override
  public long updateResource(String[] file, Resource resource) throws IOException {
    long lastModified = System.currentTimeMillis();
    RAMDir current = root;
    for (int i = 0;i < file.length;i++) {
      String name = file[i];
      RAMPath next = current.children.get(name);
      if (i == file.length - 1) {
        if (next == null) {
          current.children.put(name, next = new RAMFile(current, name, resource));
          next.parent = current;
        } else if (next instanceof RAMFile) {
          ((RAMFile)next).resource = new Timestamped<Resource>(lastModified, resource);
        } else {
          throw new IOException("A file already exist");
        }
        return next.getLastModified();
      } else {
        if (next == null) {
          current.children.put(name, next = new RAMDir(current, name, lastModified));
          next.parent = current;
          current = (RAMDir)next;
        } else if (next instanceof RAMDir) {
          current = (RAMDir)next;
        } else {
          throw new IOException("A file already exist");
        }
      }
    }
    throw new IOException();
  }

  @Override
  public Timestamped<Resource> getResource(String[] file) throws IOException {
    if (file == null) {
      throw new NullPointerException("No null file argument accepted");
    }
    RAMPath path = get(file);
    return path instanceof RAMFile ? ((RAMFile)path).resource : null;
  }

  @Override
  public boolean equals(String[] left, String[] right) {
    return Arrays.equals(left, right);
  }

  @Override
  public String getName(String[] path) {
    return path.length > 0 ? path[path.length - 1] : "";
  }

  @Override
  public Iterable<String> getNames(String[] path) {
    return Tools.iterable(path);
  }

  @Override
  public Iterator<String[]> getChildren(String[] dir) throws IOException {
    RAMPath path = get(dir);
    if (path instanceof RAMDir) {
      RAMDir a = (RAMDir)path;
      ArrayList<String[]> children = new ArrayList<String[]>(a.children.size());
      for (RAMPath child : a.children.values()) {
        children.add(child.names);
      }
      return children.iterator();
    } else {
      return Collections.<String[]>emptyList().iterator();
    }
  }

  @Override
  public String[] getChild(String[] dir, String name) throws IOException {
    RAMPath path = get(dir);
    if (path instanceof RAMDir) {
      RAMPath child = ((RAMDir)path).children.get(name);
      return child != null ? child.names : null;
    } else {
      return null;
    }
  }

  @Override
  public PathType typeOf(String[] path) throws IOException {
    RAMPath foo = get(path);
    if (foo instanceof RAMDir) {
      return PathType.DIR;
    } else if (foo instanceof RAMFile) {
      return PathType.FILE;
    } else {
      return null;
    }
  }

  @Override
  public long getLastModified(String[] path) throws IOException {
    RAMPath foo = get(path);
    return foo != null ? foo.getLastModified() : 0;
  }

  @Override
  public File getFile(String[] path) {
    return null;
  }

  @Override
  public URL getURL(String[] path) throws NullPointerException, IOException {
    if (path == null) {
      throw new NullPointerException("No null path accepted");
    }
    return new URL(contextURL, Tools.join('/', path));
  }

  @Override
  public boolean removePath(String[] path) throws IOException {
    RAMPath foo = get(path);
    if (foo != null) {
      foo.parent.children.remove(foo.name);
      foo.parent = null;
    }
    return true;
  }

  @Override
  public String[] getRoot() throws IOException {
    return root.names;
  }

  /*
  @Override
  public void setContent(RAMPath file, Content content) throws IOException {
    if (file == null) {
      throw new NullPointerException("No null file");
    }
    if (file instanceof RAMFile) {
      setContent((RAMFile)file, content);
    } else {
      throw new IOException("Cannot set content of a directory");
    }
  }

  private void setContent(RAMFile file, Content content) {
    if (content == null) {
      throw new NullPointerException("No null content");
    }
    if (file.parent.lastModified == 0) {
      file.parent.touch();
    }
    file.content = content;
  }

  @Override
  public void removePath(RAMPath path) throws IOException {
    if (path == root) {
      throw new IOException("Cannot remove root file");
    }
    if (path.parent == null) {
      throw new IOException("Cannot remove removed file");
    }
    path.parent.children.remove(path.name);
    path.parent = null;
  }

  public boolean equals(RAMPath left, RAMPath right) {
    return left == right;
  }

  public String[] getRoot() {
    return EMPTY;
  }

  public RAMDir getParent(RAMPath path) {
    return path.parent;
  }

  public String getName(RAMPath path) {
    return path.name;
  }

  public Iterator<RAMPath> getChildren(RAMPath dir) throws IOException {
    if (dir instanceof RAMDir) {
      return new ArrayList<RAMPath>(((RAMDir)dir).children.values()).iterator();
    } else {
      throw new IOException("Cannot list file");
    }
  }

  public RAMPath getChild(RAMPath dir, String name) throws IOException {
    if (dir instanceof RAMDir) {
      return ((RAMDir)dir).children.get(name);
    } else {
      throw new IOException("Cannot list file");
    }
  }

  public boolean isDir(RAMPath path) throws IOException {
    return path instanceof RAMDir;
  }

  public boolean isFile(RAMPath path) throws IOException {
    return path instanceof RAMFile;
  }

  public Content getContent(RAMPath file) throws IOException {
    return ((RAMFile)file).getContent();
  }

  public long getLastModified(RAMPath path) {
    if (path instanceof RAMDir) {
      RAMDir dir = (RAMDir)path;
      return dir.lastModified;
    } else {
      RAMFile file = (RAMFile)path;
      return file.content == null ? 0 : file.content.getLastModified();
    }
  }
*/

/*
  @Override
  public URL getURL(RAMPath path) throws IOException {
    if (path == null) {
      throw new NullPointerException("No null path accepted");
    }
    StringBuilder sb = new StringBuilder();
    pathOf(path, '/', sb);
    String spec = sb.toString();
    return new URL(contextURL, spec);
  }
*/
}
