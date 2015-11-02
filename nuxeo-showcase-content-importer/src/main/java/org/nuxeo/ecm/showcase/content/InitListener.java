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

import java.io.IOException;
import java.net.URL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.impl.blob.URLBlob;
import org.nuxeo.ecm.platform.content.template.service.PostContentCreationHandler;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:ak@nuxeo.com">Arnaud Kervern</a>
 * @since 7.10
 */
public class InitListener implements PostContentCreationHandler {

    private static final Log log = LogFactory.getLog(InitListener.class);

    private static final String SHOWCASE_CONTENT = "showcase_content.zip";

    @Override
    public void execute(CoreSession session) {
        if (Framework.isTestModeSet()) {
            return;
        }

        try {
            URL url = getClass().getClassLoader().getResource(SHOWCASE_CONTENT);
            if (url == null) {
                throw new IOException("Unable to found " + SHOWCASE_CONTENT + " resource.");
            }

            Blob blob = new URLBlob(url);
            new ShowcaseContentImporter(session).create(blob);
            log.info("Showcase content imported.");
        } catch (IOException e) {
            log.error(e, e);
        }
    }
}
