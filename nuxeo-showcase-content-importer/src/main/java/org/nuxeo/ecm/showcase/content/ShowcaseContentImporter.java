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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.platform.audit.api.AuditLogger;
import org.nuxeo.ecm.platform.audit.api.AuditReader;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.filemanager.api.FileManager;
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

    public DocumentModel importContent(String filePath) throws IOException {
        if (isImported()) {
            log.debug("Showcase Content already imported.");
        }

        File file = new File(filePath);
        if (!file.exists()) {
            throw new IOException("File " + filePath + " not found.");
        }

        FileManager importer = Framework.getLocalService(FileManager.class);
        DocumentModel doc = importer.createDocumentFromBlob(session, new FileBlob(file), getImportPathRoot(), true,
                filePath);

        markImportDone();
        return doc;
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
