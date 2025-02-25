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
package com.reandroid.dex.value;

import com.reandroid.dex.common.DexUtils;
import com.reandroid.dex.key.Key;
import com.reandroid.dex.key.PrimitiveKey;
import com.reandroid.dex.key.TypeKey;
import com.reandroid.dex.smali.SmaliWriter;
import com.reandroid.dex.smali.model.SmaliValue;
import com.reandroid.dex.smali.model.SmaliValueChar;

import java.io.IOException;

public class CharValue extends PrimitiveValueBlock {

    public CharValue(){
        super(DexValueType.CHAR);
    }

    public char get(){
        return (char) (getUnsigned() & 0xffff);
    }
    public void set(char ch){
        setUnsignedValue((0xffff & ch));
    }
    @Override
    public DexValueType<?> getValueType() {
        return DexValueType.CHAR;
    }
    @Override
    public PrimitiveKey getKey() {
        return PrimitiveKey.of(get());
    }

    @Override
    public void setKey(Key key) {
        set(((PrimitiveKey.CharKey) key).value());
    }

    @Override
    public TypeKey getDataTypeKey() {
        return TypeKey.TYPE_C;
    }

    @Override
    public void fromSmali(SmaliValue smaliValue) {
        SmaliValueChar smaliValueChar = (SmaliValueChar) smaliValue;
        set(smaliValueChar.getValue());
    }

    @Override
    public void append(SmaliWriter writer) throws IOException {
        DexUtils.appendSingleQuotedChar(writer, get());
    }
    @Override
    public String toString() {
        return DexUtils.quoteChar(get());
    }
}
