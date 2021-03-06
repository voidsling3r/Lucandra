/**
 * Copyright T Jake Luciani
 * 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package lucandra;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

import org.apache.cassandra.db.IColumn;
import org.apache.log4j.Logger;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.index.TermPositions;

public class LucandraTermDocs implements TermDocs, TermPositions
{

    private IndexReader         indexReader;
    private LucandraTermEnum    termEnum;
    private LucandraTermInfo[]  termDocs;
    private int                 docPosition;
    private int[]               termPositionArray;
    private int                 termPosition;
    private static final Logger logger = Logger.getLogger(LucandraTermDocs.class);

    public LucandraTermDocs(IndexReader indexReader)
    {
        this.indexReader = indexReader;
        termEnum = new LucandraTermEnum(indexReader);
    }

    public void close() throws IOException
    {
        // TODO Auto-generated method stub

    }

    public int doc()
    {
        if (docPosition < 0)
            docPosition = 0;

        return termDocs[docPosition].docId;
    }

    public int freq()
    {
        Integer freq = termDocs[docPosition].freq;
        termPositionArray = termDocs[docPosition].positions;
        termPosition = 0;

        return freq;
    }

    public boolean next() throws IOException
    {

        if (termDocs == null)
            return false;

        return ++docPosition < termDocs.length;
    }

    public int read(int[] docs, int[] freqs) throws IOException
    {

        int i = 0;
        for (; (termDocs != null && docPosition < termDocs.length && i < docs.length); i++, docPosition++)
        {
            docs[i] = doc();
            freqs[i] = freq();
        }

        logger.debug("read " + i);

        return i;
    }

    public void seek(Term term) throws IOException
    {

        // on a new term so check cached
        LucandraTermEnum tmp = indexReader.checkTermCache(term);
        if (tmp == null)
        {

            if (termEnum.skipTo(term))
            {
                if (termEnum.term().compareTo(term) == 0)
                {
                    termDocs = termEnum.getTermDocFreq();
                }
                else
                {
                    termDocs = null;
                }
            }
        }
        else
        {
            termEnum = tmp;
            if (termEnum.skipTo(term))
            {

                if (termEnum.term().equals(term))
                {
                    termDocs = termEnum.getTermDocFreq();
                }
                else
                {
                    termDocs = null;
                }
            }
            else
            {
                termDocs = null;
            }
        }

        docPosition = -1;
    }

    public void seek(TermEnum termEnum) throws IOException
    {
        if (termEnum instanceof LucandraTermEnum)
        {
            this.termEnum = (LucandraTermEnum) termEnum;
        }
        else
        {
            this.termEnum = (LucandraTermEnum) indexReader.terms(termEnum.term());
        }

        termDocs = this.termEnum.getTermDocFreq();

        if (logger.isDebugEnabled())
            logger.debug("seeked out " + termDocs.length);

        docPosition = -1;
    }

    public LucandraTermInfo[] filteredSeek(Term term, List<ByteBuffer> docNums)
    {

        termEnum.loadFilteredTerms(term, docNums);

        if (termEnum.getTermDocFreq() == null)
            termDocs = null;
        else
            termDocs = termEnum.getTermDocFreq();

        docPosition = -1;
        return termDocs;
    }

    // this should be used to find a already loaded doc
    public boolean skipTo(int target) throws IOException
    {
        do
        {
            if (!next())
                return false;
        }
        while (target > doc());

        return true;
    }

    public byte[] getPayload(byte[] data, int offset) throws IOException
    {
        return null;
    }

    public int getPayloadLength()
    {
        return 0;
    }

    public boolean isPayloadAvailable()
    {
        return false;
    }

    public int nextPosition() throws IOException
    {

        logger.debug("In nextPosition()");

        if (termPositionArray == null)
            return -1;

        int pos = termPositionArray[termPosition];
        termPosition++;

        if (logger.isDebugEnabled())
            logger.debug("Doc: " + doc() + ", Position: " + pos);

        return pos;
    }

}
