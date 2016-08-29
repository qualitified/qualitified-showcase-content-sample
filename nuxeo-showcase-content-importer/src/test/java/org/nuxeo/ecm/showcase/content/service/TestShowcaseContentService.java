package org.nuxeo.ecm.showcase.content.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.audit.AuditFeature;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;
import org.nuxeo.runtime.test.runner.LocalDeploy;

import java.util.List;

@RunWith(FeaturesRunner.class)
@Features({ PlatformFeature.class, AuditFeature.class })
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

        List<ShowcaseContentDescriptor> c = ((ShowcaseContentServiceImpl) showcaseContentService).getContributions();
        assertEquals(1, c.size());

        showcaseContentService.triggerImporters(session);

        DocumentModel file = session.query("select * from File").get(0);
        assertEquals("24dcbf08-0242-4729-b873-48d64b5f0011", file.getId());
    }
}
