/*
 * Copyright 2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.ImageNameSubstitutor;

/**
 * An {@link ImageNameSubstitutor} only used on CI servers to leverage internal proxy solution, that needs to vary the
 * prefix based on which container image is needed.
 * 
 * @author Greg Turnquist
 */
public class ProxyImageNameSubstitutor extends ImageNameSubstitutor {

	private static final Logger LOG = LoggerFactory.getLogger(ProxyImageNameSubstitutor.class);

	private static final List<String> NAMES_TO_PROXY_PREFIX = List.of("ryuk", "arm64v8/mariadb", "ibmcom/db2",
			"gvenzl/oracle-free", "gvenzl/oracle-xe", "mcr.microsoft.com/mssql/server");

	private static final List<String> NAMES_TO_LIBRARY_PROXY_PREFIX = List.of("mariadb", "mysql", "postgres");

	private static final String PROXY_PREFIX = "harbor-repo.vmware.com/dockerhub-proxy-cache/";

	private static final String LIBRARY_PROXY_PREFIX = PROXY_PREFIX + "library/";

	@Override
	public DockerImageName apply(DockerImageName dockerImageName) {

		if (NAMES_TO_PROXY_PREFIX.stream().anyMatch(s -> dockerImageName.asCanonicalNameString().contains(s))) {

			String transformedName = applyProxyPrefix(dockerImageName.asCanonicalNameString());
			LOG.info("Converting " + dockerImageName.asCanonicalNameString() + " to " + transformedName);
			return DockerImageName.parse(transformedName);
		}

		if (NAMES_TO_LIBRARY_PROXY_PREFIX.stream().anyMatch(s -> dockerImageName.asCanonicalNameString().contains(s))) {

			String transformedName = applyProxyAndLibraryPrefix(dockerImageName.asCanonicalNameString());
			LOG.info("Converting " + dockerImageName.asCanonicalNameString() + " to " + transformedName);
			return DockerImageName.parse(transformedName);
		}

		LOG.info("Not changing " + dockerImageName.asCanonicalNameString() + "...");
		return dockerImageName;
	}

	@Override
	protected String getDescription() {
		return "Spring Data Proxy Image Name Substitutor";
	}

	/**
	 * Apply a non-library-based prefix.
	 */
	private static String applyProxyPrefix(String imageName) {
		return PROXY_PREFIX + imageName;
	}

	/**
	 * Apply a library based prefix.
	 */
	private static String applyProxyAndLibraryPrefix(String imageName) {
		return LIBRARY_PROXY_PREFIX + imageName;
	}
}
