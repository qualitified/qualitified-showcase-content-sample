package org.nuxeo.ecm.showcase.content.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.audit.AuditFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features({ AuditFeature.class })
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy({ "org.nuxeo.ecm.content.showcase", //
        "org.nuxeo.ecm.platform.thumbnail", //
        "org.nuxeo.ecm.platform.filemanager.api", //
        "org.nuxeo.ecm.platform.collections.core", //
        "org.nuxeo.ecm.platform.filemanager.core" })
@LocalDeploy({ "org.nuxeo.ecm.content.showcase:contrib.xml" })
public class TestShowcaseContentService {

    @Inject
    protected ShowcaseContentService showcaseContentService;

    @Inject
    protected CoreSession session;

    @Test
    public void testService() {
        assertNotNull(showcaseContentService);
    }

    @Test
    public void testContribution() {
        assertEquals(0, session.query("select * from File").size());
        assertEquals(0, session.query("select * from Note").size());

        List<ShowcaseContentDescriptor> c = ((ShowcaseContentServiceImpl) showcaseContentService).getContributions();
        assertEquals(1, c.size());

        showcaseContentService.triggerImporters(session);

        DocumentModelList docs = session.query("select * from File");
        assertEquals(1, docs.size());

        docs = session.query("select * from Note where ecm:isCheckedInVersion = 0");
        assertEquals(1, docs.size());
        assertTrue(docs.get(0).getPropertyValue("dublincore:creator").equals("arthur"));
    }
}
