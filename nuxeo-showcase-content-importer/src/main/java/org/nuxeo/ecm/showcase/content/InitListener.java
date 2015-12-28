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
