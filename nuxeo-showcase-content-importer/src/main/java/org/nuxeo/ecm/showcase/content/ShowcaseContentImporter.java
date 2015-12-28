/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Nuxeo
 */

package org.nuxeo.ecm.showcase.content;

import java.io.IOException;
import java.util.Calendar;
import java.util.Collections;
import java.util.zip.ZipFile;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CloseableFile;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.io.DocumentPipe;
import org.nuxeo.ecm.core.io.DocumentReader;
import org.nuxeo.ecm.core.io.DocumentWriter;
import org.nuxeo.ecm.core.io.ExportedDocument;
import org.nuxeo.ecm.core.io.impl.DocumentPipeImpl;
import org.nuxeo.ecm.core.io.impl.plugins.NuxeoArchiveReader;
import org.nuxeo.ecm.platform.audit.api.AuditLogger;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.audit.api.Logs;
import org.nuxeo.ecm.platform.filemanager.api.FileManager;
import org.nuxeo.ecm.platform.filemanager.service.extension.ExportedZipImporter;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:ak@nuxeo.com">Arnaud Kervern</a>
 * @since 7.10
 */
public class ShowcaseContentImporter {
    public static final String INITIALIZED_EVENT = "ShowcaseContentImported";

    private static final Log log = LogFactory.getLog(ShowcaseContentImporter.class);

    protected CoreSession session;

    public ShowcaseContentImporter(CoreSession session) {
        this.session = session;
    }

    public DocumentModel create(Blob blob) throws IOException {
        if (isImported()) {
            log.debug("Showcase Content already imported.");
            return null;
        }

        FileManager importer = Framework.getLocalService(FileManager.class);
        DocumentModel doc = create(session, blob, getImportPathRoot(), true);

        markImportDone();
        return doc;
    }

    protected DocumentModel create(CoreSession documentManager, Blob content, String path, boolean overwrite)
            throws IOException {
        try (CloseableFile source = content.getCloseableFile(".zip")) {
            ZipFile zip = ExportedZipImporter.getArchiveFileIfValid(source.getFile());
            if (zip == null) {
                return null;
            }
            zip.close();

            boolean importWithIds = false;
            DocumentReader reader = new NuxeoArchiveReader(source.getFile());
            ExportedDocument root = reader.read();
            IdRef rootRef = new IdRef(root.getId());

            if (documentManager.exists(rootRef)) {
                DocumentModel target = documentManager.getDocument(rootRef);
                if (target.getPath().removeLastSegments(1).equals(new Path(path))) {
                    importWithIds = true;
                }
            }

            DocumentWriter writer = new ShowcaseWriter(documentManager, path, 10);
            reader.close();
            reader = new NuxeoArchiveReader(source.getFile());

            DocumentRef resultingRef;
            if (overwrite && importWithIds) {
                resultingRef = rootRef;
            } else {
                String rootName = root.getPath().lastSegment();
                resultingRef = new PathRef(path, rootName);
            }

            try {
                DocumentPipe pipe = new DocumentPipeImpl(10);
                pipe.setReader(reader);
                pipe.setWriter(writer);
                pipe.run();
            } catch (IOException e) {
                log.warn(e, e);
            } finally {
                reader.close();
                writer.close();
            }
            return documentManager.getDocument(resultingRef);
        }
    }

    protected boolean isImported() {
        return Framework.getService(Logs.class).getEventsCount(INITIALIZED_EVENT) > 0;
    }

    protected String getImportPathRoot() {
        return session.query("Select * from Domain").get(0).getPathAsString();
    }

    protected void markImportDone() {
        AuditLogger logger = Framework.getLocalService(AuditLogger.class);
        LogEntry entry = logger.newLogEntry();
        entry.setEventId(INITIALIZED_EVENT);
        entry.setEventDate(Calendar.getInstance().getTime());

        logger.addLogEntries(Collections.singletonList(entry));
    }
}
