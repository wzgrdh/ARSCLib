/*
  *  Copyright (C) 2022 github.com/REAndroid
  *
  *  Licensed under the Apache License, Version 2.0 (the "License");
  *  you may not use this file except in compliance with the License.
  *  You may obtain a copy of the License at
  *
  *      http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
package com.reandroid.xml;

import com.reandroid.xml.kxml2.KXmlSerializer;

import java.io.*;

public class CloseableSerializer extends KXmlSerializer implements Closeable {

    private Writer writer;
    private OutputStream outputStream;

    public CloseableSerializer(){
        super();
    }

    @Override
    public void setOutput(Writer writer){
        super.setOutput(writer);
        this.writer = writer;
    }
    @Override
    public void setOutput(OutputStream os, String encoding) throws IOException {
        super.setOutput(os, encoding);
        this.outputStream = os;
    }
    @Override
    public void endDocument() throws IOException {
        super.endDocument();
        if (getDepth() == 0) {
            close();
        }
    }
    @Override
    public void close() throws IOException {
        if(writer != null) {
            flush();
            writer.close();
            writer = null;
        }
        if(outputStream != null) {
            outputStream.close();
            outputStream = null;
        }
    }
}
