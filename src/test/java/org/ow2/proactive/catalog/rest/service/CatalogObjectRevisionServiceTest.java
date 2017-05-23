/*
 * ProActive Parallel Suite(TM):
 * The Open Source library for parallel and distributed
 * Workflows & Scheduling, Orchestration, Cloud Automation
 * and Big Data Analysis on Enterprise Grids & Clouds.
 *
 * Copyright (c) 2007 - 2017 ActiveEon
 * Contact: contact@activeeon.com
 *
 * This library is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation: version 3 of
 * the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * If needed, contact us to obtain a release under GPL Version 2 or 3
 * or a different license than the AGPL.
 */
package org.ow2.proactive.catalog.rest.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.ow2.proactive.catalog.rest.assembler.CatalogObjectRevisionResourceAssembler;
import org.ow2.proactive.catalog.rest.dto.CatalogObjectMetadata;
import org.ow2.proactive.catalog.rest.entity.Bucket;
import org.ow2.proactive.catalog.rest.entity.CatalogObject;
import org.ow2.proactive.catalog.rest.entity.CatalogObjectRevision;
import org.ow2.proactive.catalog.rest.query.QueryExpressionBuilderException;
import org.ow2.proactive.catalog.rest.service.exception.BucketNotFoundException;
import org.ow2.proactive.catalog.rest.service.exception.CatalogObjectNotFoundException;
import org.ow2.proactive.catalog.rest.service.exception.RevisionNotFoundException;
import org.ow2.proactive.catalog.rest.service.repository.BucketRepository;
import org.ow2.proactive.catalog.rest.service.repository.CatalogObjectRepository;
import org.ow2.proactive.catalog.rest.service.repository.CatalogObjectRevisionRepository;
import org.ow2.proactive.catalog.rest.util.parser.CatalogObjectParserResult;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.Link;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.io.ByteStreams;


/**
 * @author ActiveEon Team
 */
public class CatalogObjectRevisionServiceTest {

    @InjectMocks
    private CatalogObjectRevisionService workflowRevisionService;

    @Mock
    private CatalogObjectRevisionRepository workflowRevisionRepository;

    @Mock
    private CatalogObjectRepository workflowRepository;

    @Mock
    private BucketRepository bucketRepository;

    private static final Long DUMMY_ID = 0L;

    private static final Long EXISTING_ID = 1L;

    private Bucket mockedBucket;

    private SortedSet<CatalogObjectRevision> revisions;

    private CatalogObject catalogObject2Rev;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        workflowRevisionService = Mockito.spy(workflowRevisionService);

        Mockito.doReturn(new Link("test"))
               .when(workflowRevisionService)
               .createLink(Matchers.any(Long.class),
                           Matchers.any(Long.class),
                           Matchers.any(CatalogObjectRevision.class));

        Long revCnt = EXISTING_ID;
        mockedBucket = newMockedBucket(EXISTING_ID);
        revisions = new TreeSet<>();
        revisions.add(newWorkflowRevision(mockedBucket.getId(), revCnt++, LocalDateTime.now().minusHours(1)));
        revisions.add(newWorkflowRevision(mockedBucket.getId(), revCnt, LocalDateTime.now()));

        catalogObject2Rev = newMockedWorkflow(EXISTING_ID, mockedBucket, revisions, 2L);
    }

    @Test(expected = BucketNotFoundException.class)
    public void testCreateWorkflowRevisionWithInvalidBucket() throws Exception {
        when(bucketRepository.findOne(Matchers.anyLong())).thenReturn(null);
        workflowRevisionService.createCatalogObjectRevision(DUMMY_ID,
                                                            Optional.empty(),
                                                            new CatalogObjectParserResult("workflow",
                                                                                          "projectName",
                                                                                          "name",
                                                                                          ImmutableList.of()),
                                                            Optional.empty(),
                                                            new byte[0]);
    }

    @Test
    public void testCreateWorkflowWithGenericInfosAndVariables1() throws IOException {
        createWorkflow("WR-NAME-GI-VARS", "WR-PROJ-NAME-GI-VARS", "workflow.xml", Optional.empty(), Optional.empty());
        // assertions are done in the called method
    }

    @Test
    public void testCreateWorkflowWithGenericInfosAndVariables2() throws IOException {
        createWorkflow("WR-NAME-GI-VARS",
                       "WR-PROJ-NAME-GI-VARS",
                       "workflow.xml",
                       Optional.of(EXISTING_ID),
                       Optional.empty());
        // assertions are done in the called method
    }

    @Test
    public void testCreateWorkflowWithoutGenericInfosOrVariables1() throws IOException {
        createWorkflow("WR-NAME",
                       "WR-PROJ-NAME",
                       "workflow-no-generic-information-no-variable.xml",
                       Optional.empty(),
                       Optional.empty());
        // assertions are done in the called method
    }

    @Test
    public void testCreateWorkflowWithoutGenericInfosOrVariables2() throws IOException {
        createWorkflow("WR-NAME",
                       "WR-PROJ-NAME",
                       "workflow-no-generic-information-no-variable.xml",
                       Optional.of(EXISTING_ID),
                       Optional.empty());
        // assertions are done in the called method
    }

    @Test
    public void testCreateWorkflowWithLayout() throws IOException {
        createWorkflow("WR-NAME-GI-VARS",
                       "WR-PROJ-NAME-GI-VARS",
                       "workflow.xml",
                       Optional.empty(),
                       Optional.of("{\"offsets\":{\"Linux_Bash_Task\":{\"top\":" +
                                   "222,\"left\":681.5}},\"project\":\"Deployment\",\"detailedView\":true}"));
        // assertions are done in the called method
    }

    @Test(expected = CatalogObjectNotFoundException.class)
    public void testFindWorkflowInvalidId() throws Exception {
        when(workflowRepository.findOne(anyLong())).thenReturn(null);
        workflowRevisionService.findWorkflow(DUMMY_ID);
    }

    @Test
    public void testFindWorkflow() throws Exception {
        when(workflowRepository.findOne(EXISTING_ID)).thenReturn(mock(CatalogObject.class));
        workflowRevisionService.findWorkflow(EXISTING_ID);
        verify(workflowRepository, times(1)).findOne(EXISTING_ID);
    }

    @Test
    public void testFindBucketExisting() {
        Bucket bucket = mock(Bucket.class);
        when(bucketRepository.findOne(EXISTING_ID)).thenReturn(bucket);
        assertEquals(workflowRevisionService.findBucket(EXISTING_ID), bucket);
    }

    @Test(expected = BucketNotFoundException.class)
    public void testFindBucketNonExisting() {
        workflowRevisionService.findBucket(DUMMY_ID);
    }

    @Test
    public void testListWorkflowsWithWorkflowId() throws Exception {
        listWorkflows(Optional.of(DUMMY_ID));
        verify(workflowRevisionRepository, times(1)).getRevisions(anyLong(), any(Pageable.class));
    }

    @Test
    public void testListWorkflowsWithoutWorkflowId() throws Exception {
        listWorkflows(Optional.empty());
        verify(workflowRepository, times(1)).getMostRecentRevisions(anyLong(), any(Pageable.class));
    }

    @Test(expected = RevisionNotFoundException.class)
    public void testGetWorkflowWithInvalidRevisionId() throws Exception {
        when(bucketRepository.findOne(anyLong())).thenReturn(mock(Bucket.class));
        when(workflowRepository.findOne(anyLong())).thenReturn(mock(CatalogObject.class));
        when(workflowRepository.getMostRecentCatalogObjectRevision(anyLong(), anyLong())).thenReturn(null);
        workflowRevisionService.getCatalogObject(DUMMY_ID, DUMMY_ID, Optional.empty(), Optional.empty());
    }

    @Test
    public void testGetWorkflowWithValidRevisionIdNoPayload() throws Exception {
        getWorkflow(Optional.of(DUMMY_ID), Optional.empty());
        verify(workflowRevisionRepository, times(1)).getCatalogObjectRevision(DUMMY_ID, DUMMY_ID, DUMMY_ID);
    }

    private void getWorkflow(Optional<Long> revisionId, Optional<String> alt) throws IOException {

        CatalogObject mockedWf = mock(CatalogObject.class);
        when(mockedWf.getId()).thenReturn(DUMMY_ID);

        CatalogObjectRevision wfRev = new CatalogObjectRevision("workflow",
                                                                DUMMY_ID,
                                                                DUMMY_ID,
                                                                "WR-TEST",
                                                                "WR-PROJ-NAME",
                                                                LocalDateTime.now(),
                                                                null,
                                                                Lists.newArrayList(),
                                                                getWorkflowAsByteArray("workflow.xml"));
        wfRev.setCatalogObject(mockedWf);

        when(bucketRepository.findOne(anyLong())).thenReturn(mock(Bucket.class));
        when(workflowRepository.findOne(anyLong())).thenReturn(mock(CatalogObject.class));

        if (revisionId.isPresent()) {
            when(workflowRevisionRepository.getCatalogObjectRevision(anyLong(),
                                                                     anyLong(),
                                                                     anyLong())).thenReturn(wfRev);
        } else {
            when(workflowRepository.getMostRecentCatalogObjectRevision(anyLong(), anyLong())).thenReturn(wfRev);
        }

        workflowRevisionService.getCatalogObject(DUMMY_ID, DUMMY_ID, revisionId, alt);

        verify(bucketRepository, times(1)).findOne(DUMMY_ID);
        verify(workflowRepository, times(1)).findOne(DUMMY_ID);
    }

    @Test
    public void testGetWorkflowWithoutRevisionINoPayload() throws Exception {
        getWorkflow(Optional.empty(), Optional.empty());
        verify(workflowRepository, times(1)).getMostRecentCatalogObjectRevision(DUMMY_ID, DUMMY_ID);
    }

    @Test
    public void testGetWorkflowWithValidRevisionIdWithPayload() throws Exception {
        getWorkflow(Optional.of(DUMMY_ID), Optional.of("xml"));
        verify(workflowRevisionRepository, times(1)).getCatalogObjectRevision(DUMMY_ID, DUMMY_ID, DUMMY_ID);
    }

    @Test
    public void testGetWorkflowWithoutValidRevisionIdWithPayload() throws Exception {
        getWorkflow(Optional.empty(), Optional.of("xml"));
        verify(workflowRepository, times(1)).getMostRecentCatalogObjectRevision(DUMMY_ID, DUMMY_ID);
    }

    @Test
    public void testDeleteWorkflowWith1Revision() throws Exception {
        CatalogObjectRevision wfRev = new CatalogObjectRevision("workflow",
                                                                DUMMY_ID,
                                                                EXISTING_ID,
                                                                "WR-TEST",
                                                                "WR-PROJ-NAME",
                                                                LocalDateTime.now(),
                                                                null,
                                                                Lists.newArrayList(),
                                                                getWorkflowAsByteArray("workflow.xml"));
        CatalogObject catalogObject1Rev = newMockedWorkflow(EXISTING_ID,
                                                            mockedBucket,
                                                            new TreeSet<CatalogObjectRevision>() {
                                                                {
                                                                    add(wfRev);
                                                                }
                                                            },
                                                            EXISTING_ID);
        when(workflowRepository.findOne(EXISTING_ID)).thenReturn(catalogObject1Rev);
        when(workflowRepository.getMostRecentCatalogObjectRevision(mockedBucket.getId(),
                                                                   catalogObject1Rev.getId())).thenReturn(wfRev);
        workflowRevisionService.delete(mockedBucket.getId(), EXISTING_ID, Optional.empty());
        verify(workflowRepository, times(1)).getMostRecentCatalogObjectRevision(EXISTING_ID, EXISTING_ID);
        verify(workflowRepository, times(1)).delete(catalogObject1Rev);
    }

    @Test
    public void testDeleteWorkflowWith2RevisionsNoRevisionId() throws Exception {
        when(workflowRepository.findOne(EXISTING_ID)).thenReturn(catalogObject2Rev);
        when(workflowRepository.getMostRecentCatalogObjectRevision(mockedBucket.getId(),
                                                                   catalogObject2Rev.getId())).thenReturn(revisions.first());
        workflowRevisionService.delete(mockedBucket.getId(), EXISTING_ID, Optional.empty());
        verify(workflowRepository, times(1)).getMostRecentCatalogObjectRevision(EXISTING_ID, EXISTING_ID);
        verify(workflowRepository, times(1)).delete(catalogObject2Rev);
    }

    @Test
    public void testDeleteWorkflowWith2RevisionsLastRevision() {
        Long expectedRevisionId = 2L;
        when(workflowRepository.findOne(EXISTING_ID)).thenReturn(catalogObject2Rev);
        when(workflowRepository.getMostRecentCatalogObjectRevision(mockedBucket.getId(),
                                                                   EXISTING_ID)).thenReturn(revisions.first());
        when(workflowRevisionRepository.getCatalogObjectRevision(mockedBucket.getId(),
                                                                 EXISTING_ID,
                                                                 expectedRevisionId)).thenReturn(revisions.first());
        workflowRevisionService.delete(mockedBucket.getId(), EXISTING_ID, Optional.of(expectedRevisionId));
        verify(workflowRevisionRepository, times(1)).delete(revisions.first());
    }

    @Test
    public void testDeleteWorkflowWith2RevisionsPreviousRevision() {
        when(workflowRepository.findOne(EXISTING_ID)).thenReturn(catalogObject2Rev);
        when(workflowRevisionRepository.getCatalogObjectRevision(mockedBucket.getId(),
                                                                 EXISTING_ID,
                                                                 EXISTING_ID)).thenReturn(revisions.last());
        workflowRevisionService.delete(mockedBucket.getId(), EXISTING_ID, Optional.of(EXISTING_ID));
        verify(workflowRevisionRepository, times(1)).getCatalogObjectRevision(mockedBucket.getId(),
                                                                              catalogObject2Rev.getId(),
                                                                              EXISTING_ID);
        verify(workflowRevisionRepository, times(1)).delete(revisions.last());
    }

    @Test
    public void testGetWorkflowsRevisions() {
        List<Long> idList = new ArrayList<>();
        idList.add(0L);
        idList.add(2L);
        when(bucketRepository.findOne(mockedBucket.getId())).thenReturn(mockedBucket);
        when(workflowRepository.getMostRecentCatalogObjectRevision(mockedBucket.getId(),
                                                                   0L)).thenReturn(revisions.first());
        when(workflowRepository.getMostRecentCatalogObjectRevision(mockedBucket.getId(),
                                                                   2L)).thenReturn(revisions.last());
        workflowRevisionService.getCatalogObjectsRevisions(mockedBucket.getId(), idList);
        verify(workflowRevisionService, times(1)).findBucket(mockedBucket.getId());
        verify(workflowRepository, times(1)).getMostRecentCatalogObjectRevision(mockedBucket.getId(), 0L);
        verify(workflowRepository, times(1)).getMostRecentCatalogObjectRevision(mockedBucket.getId(), 2L);
    }

    private CatalogObjectRevision newWorkflowRevision(Long bucketId, Long revisionId, LocalDateTime date)
            throws Exception {
        return new CatalogObjectRevision("workflow",
                                         bucketId,
                                         revisionId,
                                         "WR-TEST",
                                         "WR-PROJ-NAME",
                                         date,
                                         null,
                                         Lists.newArrayList(),
                                         getWorkflowAsByteArray("workflow.xml"));
    }

    private Bucket newMockedBucket(Long bucketId) {
        Bucket mockedBucket = mock(Bucket.class);
        when(mockedBucket.getId()).thenReturn(bucketId);
        return mockedBucket;
    }

    private CatalogObject newMockedWorkflow(Long id, Bucket bucket, SortedSet<CatalogObjectRevision> revisions,
            Long lastRevisionId) {
        CatalogObject catalogObject = mock(CatalogObject.class);
        when(catalogObject.getId()).thenReturn(id);
        when(catalogObject.getBucket()).thenReturn(bucket);
        when(catalogObject.getRevisions()).thenReturn(revisions);
        when(catalogObject.getLastRevisionId()).thenReturn(lastRevisionId);
        for (CatalogObjectRevision catalogObjectRevision : revisions) {
            catalogObjectRevision.setCatalogObject(catalogObject);
        }
        return catalogObject;
    }

    private void listWorkflows(Optional<Long> wId) throws QueryExpressionBuilderException {
        when(bucketRepository.findOne(anyLong())).thenReturn(mock(Bucket.class));
        when(workflowRepository.findOne(anyLong())).thenReturn(mock(CatalogObject.class));
        PagedResourcesAssembler mockedAssembler = mock(PagedResourcesAssembler.class);
        when(mockedAssembler.toResource(any(PageImpl.class),
                                        any(CatalogObjectRevisionResourceAssembler.class))).thenReturn(null);

        if (wId.isPresent()) {
            when(workflowRevisionRepository.getRevisions(anyLong(),
                                                         any(Pageable.class))).thenReturn(mock(PageImpl.class));
        } else {
            when(workflowRepository.getMostRecentRevisions(anyLong(),
                                                           any(Pageable.class))).thenReturn(mock(PageImpl.class));
        }

        workflowRevisionService.listCatalogObjects(DUMMY_ID, wId, Optional.empty(), null, mockedAssembler);
    }

    private void createWorkflow(String name, String projectName, String fileName, Optional<Long> wId,
            Optional<String> layout) throws IOException {
        String layoutStr = layout.orElse("");
        when(bucketRepository.findOne(anyLong())).thenReturn(mock(Bucket.class));
        when(workflowRevisionRepository.save(any(CatalogObjectRevision.class))).thenReturn(new CatalogObjectRevision("workflow",
                                                                                                                     EXISTING_ID,
                                                                                                                     EXISTING_ID,
                                                                                                                     name,
                                                                                                                     projectName,
                                                                                                                     LocalDateTime.now(),
                                                                                                                     layoutStr,
                                                                                                                     Lists.newArrayList(),
                                                                                                                     getWorkflowAsByteArray(fileName)));

        if (wId.isPresent()) {
            when(workflowRepository.findOne(anyLong())).thenReturn(new CatalogObject(mock(Bucket.class),
                                                                                     Lists.newArrayList()));
        }

        CatalogObjectMetadata actualWFMetadata = workflowRevisionService.createCatalogObjectRevision(DUMMY_ID,
                                                                                                     wId,
                                                                                                     getWorkflowAsByteArray(fileName),
                                                                                                     layout);

        verify(workflowRevisionRepository, times(1)).save(any(CatalogObjectRevision.class));

        assertEquals(name, actualWFMetadata.name);
        assertEquals(projectName, actualWFMetadata.projectName);
        assertEquals(EXISTING_ID, actualWFMetadata.bucketId);
        assertEquals(EXISTING_ID, actualWFMetadata.revisionId);
        if (layout.isPresent()) {
            assertEquals(layout.get(), actualWFMetadata.layout);
        }
    }

    private static byte[] getWorkflowAsByteArray(String filename) throws IOException {
        return ByteStreams.toByteArray(new FileInputStream(getWorkflowFile(filename)));
    }

    private static File getWorkflowFile(String filename) {
        return new File(CatalogObjectRevisionServiceTest.class.getResource("/workflows/" + filename).getFile());
    }
}
