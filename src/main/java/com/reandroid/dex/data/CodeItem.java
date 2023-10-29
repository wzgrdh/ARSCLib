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
package com.reandroid.dex.data;

import com.reandroid.arsc.base.BlockRefresh;
import com.reandroid.arsc.item.IntegerVisitor;
import com.reandroid.arsc.item.VisitableInteger;
import com.reandroid.arsc.io.BlockReader;
import com.reandroid.arsc.item.IntegerReference;
import com.reandroid.dex.base.*;
import com.reandroid.dex.debug.DebugParameter;
import com.reandroid.dex.id.IdItem;
import com.reandroid.dex.key.DataKey;
import com.reandroid.dex.key.Key;
import com.reandroid.dex.key.KeyItemCreate;
import com.reandroid.dex.reference.DataItemIndirectReference;
import com.reandroid.dex.id.ProtoId;
import com.reandroid.dex.ins.RegistersTable;
import com.reandroid.dex.ins.TryBlock;
import com.reandroid.dex.sections.SectionType;
import com.reandroid.dex.writer.SmaliFormat;
import com.reandroid.dex.writer.SmaliWriter;
import com.reandroid.utils.collection.CombiningIterator;
import com.reandroid.utils.collection.EmptyIterator;

import java.io.IOException;
import java.util.Iterator;
import java.util.Objects;

public class CodeItem extends DataItem implements RegistersTable, PositionAlignedItem, KeyItemCreate,
        SmaliFormat, VisitableInteger {

    private final Header header;
    private final InstructionList instructionList;
    private TryBlock tryBlock;

    private final DataKey<CodeItem> codeItemKey;

    private MethodDef methodDef;

    public CodeItem() {
        super(3);
        this.header = new Header(this);
        this.instructionList = new InstructionList(this);
        this.tryBlock = null;

        this.codeItemKey = new DataKey<>(this);

        addChild(0, header);
        addChild(1, instructionList);
    }

    @Override
    public DataKey<CodeItem> getKey() {
        return codeItemKey;
    }
    @SuppressWarnings("unchecked")
    @Override
    public void setKey(Key key){
        DataKey<CodeItem> codeItemKey = (DataKey<CodeItem>) key;
        merge(codeItemKey.getItem());
    }

    @Override
    public void visitIntegers(IntegerVisitor visitor) {
        getInstructionList().visitIntegers(visitor);
    }
    @Override
    public int getRegistersCount(){
        return header.registersCount.get();
    }
    @Override
    public void setRegistersCount(int count){
        header.registersCount.set(count);
    }
    @Override
    public int getParameterRegistersCount(){
        return header.parameterRegisters.get();
    }
    @Override
    public void setParameterRegistersCount(int count){
        header.parameterRegisters.set(count);
    }

    public DebugInfo getDebugInfo(){
        return header.debugInfoOffset.getItem();
    }
    public DebugInfo getOrCreateDebugInfo(){
        return header.debugInfoOffset.getOrCreate();
    }
    public void setDebugInfo(DebugInfo debugInfo){
        header.debugInfoOffset.setItem(debugInfo);
    }
    public InstructionList getInstructionList() {
        return instructionList;
    }
    public IntegerReference getTryCountReference(){
        return header.tryBlockCount;
    }
    public TryBlock getTryBlock(){
        return tryBlock;
    }
    public TryBlock getOrCreateTryBlock(){
        initTryBlock();
        return tryBlock;
    }

    public MethodDef getMethodDef() {
        return methodDef;
    }
    public void setMethodDef(MethodDef methodDef) {
        this.methodDef = methodDef;
    }

    IntegerReference getInstructionCodeUnitsReference(){
        return header.instructionCodeUnits;
    }
    IntegerReference getInstructionOutsReference(){
        return header.outs;
    }
    void initTryBlock(){
        if(this.tryBlock == null){
            this.tryBlock = new TryBlock(this);
            addChild(2, this.tryBlock);
        }
    }
    @Override
    public DexPositionAlign getPositionAlign(){
        if(this.tryBlock != null){
            return this.tryBlock.getPositionAlign();
        }else if(this.instructionList != null){
            return this.instructionList.getBlockAlign();
        }
        return new DexPositionAlign();
    }
    @Override
    public void removeLastAlign(){
        if(this.tryBlock != null){
            this.tryBlock.getPositionAlign().setSize(0);
        }else if(this.instructionList != null){
            this.instructionList.getBlockAlign().setSize(0);
        }
    }

    @Override
    public void removeSelf() {
        super.removeSelf();
        //TryBlock tryBlock = this.tryBlock;
        //if(tryBlock != null){
          //  this.tryBlock = null;
            //tryBlock.onRemove();
        //}
        //this.instructionList.onRemove();
        //this.header.onRemove();
    }

    public Iterator<IdItem> usedIds(){
        DebugInfo debugInfo = getDebugInfo();
        Iterator<IdItem> iterator1;
        if(debugInfo == null){
            iterator1 = EmptyIterator.of();
        }else {
            iterator1 = debugInfo.usedIds();
        }
        return CombiningIterator.two(iterator1, getInstructionList().usedIds());
    }
    public void merge(CodeItem codeItem){
        if(codeItem == this){
            return;
        }
        this.header.merge(codeItem.header);
        getInstructionList().merge(codeItem.getInstructionList());
        TryBlock comingTry = codeItem.getTryBlock();
        if(comingTry != null){
            TryBlock tryBlock = getOrCreateTryBlock();
            tryBlock.merge(comingTry);
        }
    }
    @Override
    public void append(SmaliWriter writer) throws IOException {
        MethodDef methodDef = getMethodDef();
        DebugInfo debugInfo = getDebugInfo();
        ProtoId proto = methodDef.getMethodId().getProto();
        writer.newLine();
        writer.append(".locals ");
        InstructionList instructionList = getInstructionList();
        int count = getRegistersCount() - getParameterRegistersCount();
        writer.append(count);
        methodDef.appendParameterAnnotations(writer, proto);
        if(debugInfo != null){
            Iterator<DebugParameter> iterator = debugInfo.getParameters();
            while (iterator.hasNext()){
                iterator.next().append(writer);
            }
        }
        methodDef.appendAnnotations(writer);
        instructionList.append(writer);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        CodeItem codeItem = (CodeItem) obj;
        return header.equals(codeItem.header) &&
                instructionList.equals(codeItem.instructionList) &&
                Objects.equals(tryBlock, codeItem.tryBlock);
    }

    @Override
    public int hashCode() {
        int hash = header.hashCode();
        hash = hash * 31 + instructionList.hashCode();
        hash = hash * 31;
        TryBlock tryBlock = this.tryBlock;
        if(tryBlock != null){
            hash = hash + tryBlock.hashCode();
        }
        return hash;
    }

    @Override
    public String toString() {
        if(isNull()){
            return "NULL";
        }
        return header.toString()
                + "\n instructionList=" + instructionList
                + "\n tryBlock=" + tryBlock
                + "\n debug=" + getDebugInfo();
    }

    static class Header extends DexBlockItem implements BlockRefresh {

        private final CodeItem codeItem;

        final IntegerReference registersCount;
        final IntegerReference parameterRegisters;
        final IntegerReference outs;
        final IntegerReference tryBlockCount;

        final DataItemIndirectReference<DebugInfo> debugInfoOffset;
        final IntegerReference instructionCodeUnits;

        public Header(CodeItem codeItem) {
            super(16);
            this.codeItem = codeItem;
            int offset = -2;
            this.registersCount = new IndirectShort(this, offset += 2);
            this.parameterRegisters = new IndirectShort(this, offset += 2);
            this.outs = new IndirectShort(this, offset += 2);
            this.tryBlockCount = new IndirectShort(this, offset += 2);
            this.debugInfoOffset = new DataItemIndirectReference<>(SectionType.DEBUG_INFO,this, offset += 2, UsageMarker.USAGE_DEBUG);
            this.instructionCodeUnits = new IndirectInteger(this, offset + 4);
        }


        @Override
        public void refresh() {
            debugInfoOffset.refresh();
        }
        @Override
        public void onReadBytes(BlockReader reader) throws IOException {
            super.onReadBytes(reader);
            this.debugInfoOffset.updateItem();
            if(this.tryBlockCount.get() != 0){
                this.codeItem.initTryBlock();
            }
        }

        public void onRemove(){
            debugInfoOffset.setItem((DebugInfo) null);
        }

        public void merge(Header header){
            registersCount.set(header.registersCount.get());
            parameterRegisters.set(header.parameterRegisters.get());
            outs.set(header.outs.get());
            tryBlockCount.set(header.tryBlockCount.get());
            DebugInfo comingDebug = header.debugInfoOffset.getItem();
            if(comingDebug != null){
                debugInfoOffset.setItem(comingDebug.getKey());
            }
            instructionCodeUnits.set(header.instructionCodeUnits.get());
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            Header header = (Header) obj;
            return registersCount.get() == header.registersCount.get() &&
                    parameterRegisters.get() == header.parameterRegisters.get() &&
                    outs.get() == header.outs.get() &&
                    tryBlockCount.get() == header.tryBlockCount.get() &&
                    instructionCodeUnits.get() == header.instructionCodeUnits.get() &&
                    Objects.equals(debugInfoOffset.getItem(), header.debugInfoOffset.getItem());
        }

        @Override
        public int hashCode() {
            int hash = 1;
            hash = hash * 31 + registersCount.get();
            hash = hash * 31 + parameterRegisters.get();
            hash = hash * 31 + outs.get();
            hash = hash * 31 + tryBlockCount.get();
            hash = hash * 31 + instructionCodeUnits.get();
            hash = hash * 31;
            DebugInfo info = debugInfoOffset.getItem();
            if(info != null){
                hash = hash + info.hashCode();
            }
            return hash;
        }

        @Override
        public String toString() {
            return  "registers=" + registersCount +
                    ", parameters=" + parameterRegisters +
                    ", outs=" + outs +
                    ", tries=" + tryBlockCount +
                    ", debugInfo=" + debugInfoOffset +
                    ", codeUnits=" + instructionCodeUnits;
        }
    }
}
