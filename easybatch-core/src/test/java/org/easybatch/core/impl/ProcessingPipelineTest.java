/*
 *  The MIT License
 *
 *   Copyright (c) 2015, Mahmoud Ben Hassine (mahmoud@benhassine.fr)
 *
 *   Permission is hereby granted, free of charge, to any person obtaining a copy
 *   of this software and associated documentation files (the "Software"), to deal
 *   in the Software without restriction, including without limitation the rights
 *   to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *   copies of the Software, and to permit persons to whom the Software is
 *   furnished to do so, subject to the following conditions:
 *
 *   The above copyright notice and this permission notice shall be included in
 *   all copies or substantial portions of the Software.
 *
 *   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *   IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *   FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *   AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *   LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *   OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *   THE SOFTWARE.
 */

package org.easybatch.core.impl;

import org.easybatch.core.api.Record;
import org.easybatch.core.api.RecordProcessingException;
import org.easybatch.core.api.RecordProcessor;
import org.easybatch.core.api.Report;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ProcessingPipelineTest {

    @Mock
    private Record record;

    @Mock
    private Object preProcessedRecord, processedRecord, secondlyProcessedRecord;

    @Mock
    private Object processingResult;

    @Mock
    private RecordProcessingException exception;

    @Mock
    private Report report;

    @Mock
    private EventManager eventManager;

    @Mock
    private RecordProcessor recordProcessor1, recordProcessor2;

    private Pipeline processingPipeline;

    @Before
    public void setUp() throws Exception {
        when(eventManager.fireBeforeRecordProcessing(record)).thenReturn(preProcessedRecord);
        processingPipeline = new Pipeline(asList(recordProcessor1, recordProcessor2), report, eventManager);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testProcessWithoutException() throws Exception {
        when(recordProcessor1.processRecord(preProcessedRecord)).thenReturn(processedRecord);
        when(recordProcessor2.processRecord(processedRecord)).thenReturn(secondlyProcessedRecord);

        boolean processingError = processingPipeline.process(record);

        assertThat(processingError).isFalse();

        InOrder inOrder = inOrder(eventManager, recordProcessor1, recordProcessor2);

        inOrder.verify(eventManager).fireBeforeRecordProcessing(record);
        inOrder.verify(recordProcessor1).processRecord(preProcessedRecord);
        inOrder.verify(recordProcessor2).processRecord(processedRecord);
        inOrder.verify(eventManager).fireAfterRecordProcessing(record, secondlyProcessedRecord);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testProcessWithException() throws Exception {
        when(recordProcessor1.processRecord(preProcessedRecord)).thenThrow(exception);

        boolean processingError = processingPipeline.process(record);

        assertThat(processingError).isTrue();

        InOrder inOrder = inOrder(eventManager, report, recordProcessor1, recordProcessor2);

        inOrder.verify(eventManager).fireBeforeRecordProcessing(record);
        inOrder.verify(recordProcessor1).processRecord(preProcessedRecord);
        inOrder.verify(report).incrementTotalErrorRecord();
        inOrder.verify(eventManager).fireOnRecordProcessingException(record, exception);

        verifyZeroInteractions(recordProcessor2);
    }

    @Test
    public void testGetLastProcessor() throws Exception {
        assertThat(processingPipeline.getLastProcessor()).isEqualTo(recordProcessor2);
    }
}
