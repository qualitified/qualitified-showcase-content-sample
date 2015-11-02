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

import java.io.File;
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
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.io.DocumentPipe;
import org.nuxeo.ecm.core.io.DocumentReader;
import org.nuxeo.ecm.core.io.DocumentWriter;
import org.nuxeo.ecm.core.io.ExportedDocument;
import org.nuxeo.ecm.core.io.impl.DocumentPipeImpl;
import org.nuxeo.ecm.core.io.impl.plugins.NuxeoArchiveReader;
import org.nuxeo.ecm.platform.audit.api.AuditLogger;
import org.nuxeo.ecm.platform.audit.api.AuditReader;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
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

    public DocumentModel create(String filePath) throws IOException {
        if (isImported()) {
            log.debug("Showcase Content already imported.");
        }

        File file = new File(filePath);
        if (!file.exists()) {
            throw new IOException("File " + filePath + " not found.");
        }

        FileManager importer = Framework.getLocalService(FileManager.class);
        DocumentModel doc = create(session, new FileBlob(file), getImportPathRoot(), true);

        markImportDone();
        return doc;
    }

    protected DocumentModel create(CoreSession documentManager, Blob content, String path, boolean overwrite)
            throws IOException {
        try (CloseableFile source = content.getCloseableFile()) {
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
        AuditReader audit = Framework.getLocalService(AuditReader.class);
        return !audit.nativeQuery(String.format("from LogEntry log where log.eventId='%s'", INITIALIZED_EVENT), 0, 1)
                     .isEmpty();
    }

    protected String getImportPathRoot() {
        return session.getRootDocument().getPathAsString();
    }

    protected void markImportDone() {
        AuditLogger logger = Framework.getLocalService(AuditLogger.class);
        LogEntry entry = logger.newLogEntry();
        entry.setEventId(INITIALIZED_EVENT);
        entry.setEventDate(Calendar.getInstance().getTime());

        logger.addLogEntries(Collections.singletonList(entry));
    }
}
