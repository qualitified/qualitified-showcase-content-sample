/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo
 */

package org.nuxeo.ecm.showcase.content;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.impl.blob.URLBlob;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.audit.AuditFeature;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

/**
 * @author <a href="mailto:ak@nuxeo.com">Arnaud Kervern</a>
 * @since 7.10
 */
@RunWith(FeaturesRunner.class)
@Features({ PlatformFeature.class, AuditFeature.class })
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy({ "org.nuxeo.ecm.content.showcase", //
        "org.nuxeo.ecm.platform.thumbnail", //
        "org.nuxeo.ecm.platform.filemanager.api", //
        "org.nuxeo.ecm.platform.collections.core", //
        "org.nuxeo.ecm.platform.filemanager.core" })
public class TestShowcaseContentImporter {

    @Inject
    CoreSession session;

    @Test
    public void testImportFile() throws IOException, URISyntaxException {
        DocumentModelList documentModels = session.query("Select * from Document");
        int docsSize = documentModels.size();

        URL resource = getClass().getClassLoader().getResource("export.zip");
        assertNotNull(resource);

        ShowcaseContentImporter importer = new ShowcaseContentImporter(session);

        assertFalse(importer.isImported());
        importer.create(new URLBlob(resource));

        // assertTrue(importer.isImported());
        assertNotEquals(docsSize, session.query("select * from Document").size());

        DocumentModel file = session.query("select * from File").get(0);
        assertEquals("24dcbf08-0242-4729-b873-48d64b5f0011", file.getId());
    }
}
