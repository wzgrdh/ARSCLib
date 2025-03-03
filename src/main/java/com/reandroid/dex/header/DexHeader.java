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
package com.reandroid.dex.header;

import com.reandroid.arsc.base.Block;
import com.reandroid.arsc.base.DirectStreamReader;
import com.reandroid.arsc.base.OffsetSupplier;
import com.reandroid.arsc.io.BlockReader;
import com.reandroid.arsc.item.IntegerItem;
import com.reandroid.arsc.item.IntegerReference;
import com.reandroid.dex.sections.SectionType;
import com.reandroid.dex.sections.SpecialItem;
import com.reandroid.utils.io.FileUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class DexHeader extends SpecialItem implements OffsetSupplier, DirectStreamReader {

    public final Magic magic;
    public final DexVersion version;
    public final DexChecksum checksum;
    public final Signature signature;

    public final IntegerReference fileSize;
    public final IntegerReference headerSize;
    public final Endian endian;
    public final IntegerReference map;

    public final CountAndOffset string_id;
    public final CountAndOffset type_id;
    public final CountAndOffset proto_id;
    public final CountAndOffset field_id;
    public final CountAndOffset method_id;
    public final CountAndOffset class_id;
    public final CountAndOffset data;
    public final DexContainerInfo containerInfo;

    /**
     * A placeholder for unknown bytes. Normally the size should be zero, but a tampered dex or
     * future versions may have extra bytes than declared on headerSize.
     * */
    public final UnknownHeaderBytes unknown;

    public DexHeader() {
        super(17);

        this.magic = new Magic();
        this.version = new DexVersion();
        this.checksum = new DexChecksum();
        this.signature = new Signature();

        this.fileSize = new IntegerItem();
        this.headerSize = new IntegerItem();

        this.endian = new Endian();

        this.map = new IntegerItem();

        this.string_id = new CountAndOffset();
        this.type_id = new CountAndOffset();
        this.proto_id = new CountAndOffset();
        this.field_id = new CountAndOffset();
        this.method_id = new CountAndOffset();
        this.class_id = new CountAndOffset();
        this.data = new CountAndOffset();
        this.containerInfo = new DexContainerInfo();

        this.unknown = new UnknownHeaderBytes();

        addChildBlock(0, magic);
        addChildBlock(1, version);
        addChildBlock(2, checksum);
        addChildBlock(3, signature);
        addChildBlock(4, (Block) fileSize);
        addChildBlock(5, (Block) headerSize);
        addChildBlock(6, endian);
        addChildBlock(7, (Block) map);

        addChildBlock(8, string_id);
        addChildBlock(9, type_id);
        addChildBlock(10, proto_id);
        addChildBlock(11, field_id);
        addChildBlock(12, method_id);
        addChildBlock(13, class_id);
        addChildBlock(14, data);
        addChildBlock(15, containerInfo);

        addChildBlock(16, unknown);

        setOffsetReference(containerInfo.getOffsetReference());
    }

    @Override
    public SectionType<DexHeader> getSectionType() {
        return SectionType.HEADER;
    }

    public int getVersion(){
        return version.getVersionAsInteger();
    }
    public void setVersion(int version){
        this.version.setVersionAsInteger(version);
    }
    public CountAndOffset get(SectionType<?> sectionType){
        if(sectionType == SectionType.STRING_ID){
            return string_id;
        }
        if(sectionType == SectionType.TYPE_ID){
            return type_id;
        }
        if(sectionType == SectionType.PROTO_ID){
            return proto_id;
        }
        if(sectionType == SectionType.FIELD_ID){
            return field_id;
        }
        if(sectionType == SectionType.METHOD_ID){
            return method_id;
        }
        if(sectionType == SectionType.CLASS_ID){
            return class_id;
        }
        return null;
    }
    @Override
    public IntegerReference getOffsetReference() {
        return containerInfo.getOffsetReference();
    }
    @Override
    public void setOffsetReference(IntegerReference reference) {
        if (reference != containerInfo.getOffsetReference()) {
            throw new RuntimeException("Header already has offset reference");
        }
    }

    public int getFileSize() {
        return fileSize.get();
    }
    @Override
    protected boolean isValidOffset(int offset){
        return offset >= 0;
    }

    public boolean isClassDefinitionOrderEnforced(){
        return version.isClassDefinitionOrderEnforced();
    }
    public boolean isMultiLayoutVersion() {
        return version.isMultiLayoutVersion();
    }

    @Override
    public int readBytes(InputStream inputStream) throws IOException {
        int result = 0;
        int size = 17;
        for (int i = 0; i < size; i++) {
            Block block = getChildBlockAt(i);
            result += ((DirectStreamReader) block).readBytes(inputStream);
        }
        return result;
    }
    @Override
    public void onReadBytes(BlockReader reader) throws IOException {
        super.nonCheckRead(reader);
    }

    @Override
    protected void onRefreshed() {
        super.onRefreshed();
        this.headerSize.set(countBytes());
    }

    /**
     * Updates header checksum with alder32 algorithm
     * returns true if the value of checksum is changed, otherwise false
     * */
    public boolean updateChecksum() {
        return this.checksum.update();
    }

    /**
     * Updates header signature with sha1 algorithm
     * */
    public void updateSignature() {
        this.signature.update();
    }

    @Override
    public String toString() {
        return "Header {" +
                "magic=" + magic +
                ", version=" + version +
                ", checksum=" + checksum +
                ", signature=" + signature +
                ", fileSize=" + fileSize +
                ", headerSize=" + headerSize +
                ", endian=" + endian +
                ", map=" + map +
                ", strings=" + string_id +
                ", type=" + type_id +
                ", proto=" + proto_id +
                ", field=" + field_id +
                ", method=" + method_id +
                ", clazz=" + class_id +
                ", data=" + data +
                ", container-v41" + containerInfo +
                ", unknown=" + unknown +
                '}';
    }

    public static DexHeader readHeader(File file) throws IOException {
        InputStream inputStream = FileUtil.inputStream(file);
        DexHeader dexHeader = readHeader(inputStream);
        inputStream.close();
        return dexHeader;
    }
    public static DexHeader readHeader(InputStream inputStream) throws IOException {
        DexHeader dexHeader = new DexHeader();
        dexHeader.magic.setDisableVerification(true);
        int read = dexHeader.readBytes(inputStream);
        if(read < dexHeader.countBytes()) {
            throw new IOException("Few bytes to read header: " + read);
        }
        return dexHeader;
    }
    public static DexHeader readHeader(byte[] bytes) throws IOException {
        DexHeader dexHeader = new DexHeader();
        dexHeader.magic.setDisableVerification(true);
        if(bytes.length < dexHeader.countBytes()) {
            throw new IOException("Few bytes to read header: " + bytes.length);
        }
        dexHeader.readBytes(new BlockReader(bytes));
        return dexHeader;
    }
}
