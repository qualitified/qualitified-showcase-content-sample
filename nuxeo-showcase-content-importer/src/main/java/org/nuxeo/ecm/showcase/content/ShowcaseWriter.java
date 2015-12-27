/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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

import static org.nuxeo.ecm.platform.picture.api.ImagingDocumentConstants.CTX_FORCE_VIEWS_GENERATION;
import static org.nuxeo.ecm.platform.picture.api.ImagingDocumentConstants.PICTURE_FACET;
import static org.nuxeo.ecm.platform.video.VideoConstants.CTX_FORCE_INFORMATIONS_GENERATION;
import static org.nuxeo.ecm.platform.video.VideoConstants.VIDEO_FACET;

import java.util.Collections;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Element;
import org.nuxeo.common.collections.ScopeType;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.io.ExportConstants;
import org.nuxeo.ecm.core.io.ExportedDocument;
import org.nuxeo.ecm.core.io.impl.plugins.DocumentModelWriter;
import org.nuxeo.ecm.core.versioning.VersioningService;

/**
 * @author <a href="mailto:ak@nuxeo.com">Arnaud Kervern</a>
 * @since 7.10
 */
public class ShowcaseWriter extends DocumentModelWriter {

    private static final Log log = LogFactory.getLog(ShowcaseWriter.class);

    public ShowcaseWriter(CoreSession session, String parentPath, int saveInterval) {
        super(session, parentPath, saveInterval);
    }

    /**
     * Import a new document given its path keeping his id
     * <p>
     * The parent of this document is assumed to exist.
     *
     * @param xdoc the document containing
     * @param toPath the path of the doc to create
     */
    protected DocumentModel createDocument(ExportedDocument xdoc, Path toPath) {
        Path parentPath = toPath.removeLastSegments(1);
        String name = toPath.lastSegment();

        DocumentModel doc = new DocumentModelImpl(null, xdoc.getType(), xdoc.getId(), toPath, null, null, new PathRef(
                parentPath.toString()), null, null, null, null);

        // set lifecycle state at creation
        Element system = xdoc.getDocument().getRootElement().element(ExportConstants.SYSTEM_TAG);
        String lifeCycleState = system.element(ExportConstants.LIFECYCLE_STATE_TAG).getText();
        doc.putContextData(CoreSession.IMPORT_LIFECYCLE_STATE, lifeCycleState);
        String lifeCyclePolicy = system.element(ExportConstants.LIFECYCLE_POLICY_TAG).getText();
        doc.putContextData(CoreSession.IMPORT_LIFECYCLE_POLICY, lifeCyclePolicy);

        // loadFacets before schemas so that additional schemas are not skipped
        loadFacetsInfo(doc, xdoc.getDocument());

        // then load schemas data
        loadSchemas(xdoc, doc, xdoc.getDocument());

        if (doc.hasSchema("uid")) {
            doc.putContextData(ScopeType.REQUEST, VersioningService.SKIP_VERSIONING, true);
        }

        // XXX Not used, as we override the listener; but it is the right way to force video informations generation.
        if (doc.hasFacet(VIDEO_FACET)) {
            doc.putContextData(CTX_FORCE_INFORMATIONS_GENERATION, true);
        }

        if (doc.hasFacet(PICTURE_FACET)) {
            doc.putContextData(CTX_FORCE_VIEWS_GENERATION, true);
        }

        session.importDocuments(Collections.singletonList(doc));

        // load into the document the system properties, document needs to exist
        loadSystemInfo(doc, xdoc.getDocument());

        unsavedDocuments += 1;
        saveIfNeeded();

        return doc;
    }
}
