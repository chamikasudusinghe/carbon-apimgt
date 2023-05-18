/*
 * Copyright (c) 2023, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */


package org.wso2.carbon.apimgt.internal.service.impl;

import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.apimgt.api.ExceptionCodes;
import org.wso2.carbon.apimgt.impl.APIConstants;
import org.wso2.carbon.apimgt.impl.dto.RuntimeArtifactDto;
import org.wso2.carbon.apimgt.impl.gatewayartifactsynchronizer.RuntimeArtifactGeneratorUtil;
import org.wso2.carbon.apimgt.internal.service.RetrieveRuntimeArtifactsApiService;
import org.wso2.carbon.apimgt.internal.service.dto.SynapseArtifactListDTO;
import org.wso2.carbon.apimgt.internal.service.dto.UUIDListDTO;
import org.wso2.carbon.apimgt.rest.api.common.RestApiConstants;
import org.wso2.carbon.apimgt.rest.api.util.utils.RestApiUtil;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.File;
import java.nio.file.Files;
import java.util.List;

/**
 * Runtime Artifact Service implementation.
 */
public class RetrieveRuntimeArtifactsApiServiceImpl implements RetrieveRuntimeArtifactsApiService {

    @Override
    public Response retrieveRuntimeArtifactsGet(String type, String dataPlaneId, String gatewayAccessibilityType,
                                                MessageContext messageContext)
            throws APIManagementException {
        RuntimeArtifactDto runtimeArtifactDto;
        String organization = RestApiUtil.getOrganization(messageContext);
        if (StringUtils.isEmpty(organization)) {
            return Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(RestApiUtil.getErrorDTO(ExceptionCodes.ORGANIZATION_NOT_FOUND))
                    .build();
        }
        if (organization.equalsIgnoreCase(APIConstants.ORG_ALL_QUERY_PARAM)) {
            runtimeArtifactDto = RuntimeArtifactGeneratorUtil.generateAllRuntimeArtifact(dataPlaneId,
                    gatewayAccessibilityType, type);
        } else {
            runtimeArtifactDto = RuntimeArtifactGeneratorUtil.generateAllRuntimeArtifact(organization, dataPlaneId,
                    gatewayAccessibilityType, type);
        }
        if (runtimeArtifactDto != null) {
            if (runtimeArtifactDto.isFile()) {
                File artifact = (File) runtimeArtifactDto.getArtifact();
                StreamingOutput streamingOutput = (outputStream) -> {
                    try {
                        Files.copy(artifact.toPath(), outputStream);
                    } finally {
                        Files.delete(artifact.toPath());
                    }
                };
                return Response.ok(streamingOutput).header(RestApiConstants.HEADER_CONTENT_DISPOSITION,
                        "attachment; filename=apis.zip").header(RestApiConstants.HEADER_CONTENT_TYPE,
                        APIConstants.APPLICATION_ZIP).build();
            } else {
                SynapseArtifactListDTO synapseArtifactListDTO = new SynapseArtifactListDTO();
                if (runtimeArtifactDto.getArtifact() instanceof List) {
                    synapseArtifactListDTO.setList((List<String>) runtimeArtifactDto.getArtifact());
                    synapseArtifactListDTO.setCount(((List<String>) runtimeArtifactDto.getArtifact()).size());
                }
                return Response.ok().entity(synapseArtifactListDTO)
                        .header(RestApiConstants.HEADER_CONTENT_TYPE, RestApiConstants.APPLICATION_JSON).build();
            }
        } else {
            return Response
                    .status(Response.Status.NOT_FOUND)
                    .entity(RestApiUtil.getErrorDTO(ExceptionCodes.NO_API_ARTIFACT_FOUND))
                    .build();
        }
    }

    @Override
    public Response retrieveRuntimeArtifactsPost(String type, String dataPlaneId, String gatewayAccessibilityType, UUIDListDTO uuidList, MessageContext messageContext) throws APIManagementException {
        RuntimeArtifactDto runtimeArtifactDto;
        String organization = RestApiUtil.getOrganization(messageContext);
        if (StringUtils.isEmpty(organization)) {
            return Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(RestApiUtil.getErrorDTO(ExceptionCodes.ORGANIZATION_NOT_FOUND))
                    .build();
        }
        if (organization.equalsIgnoreCase(APIConstants.ORG_ALL_QUERY_PARAM)) {
            runtimeArtifactDto = RuntimeArtifactGeneratorUtil.generateAllRuntimeArtifact(type, dataPlaneId,
                    gatewayAccessibilityType, uuidList.getUuids());
        } else {
            runtimeArtifactDto = RuntimeArtifactGeneratorUtil.generateAllRuntimeArtifact(organization, type, dataPlaneId,
                    gatewayAccessibilityType, uuidList.getUuids());
        }

        if (runtimeArtifactDto != null) {
            if (runtimeArtifactDto.isFile()) {
                File artifact = (File) runtimeArtifactDto.getArtifact();
                StreamingOutput streamingOutput = (outputStream) -> {
                    try {
                        Files.copy(artifact.toPath(), outputStream);
                    } finally {
                        Files.delete(artifact.toPath());
                    }
                };
                return Response.ok(streamingOutput).header(RestApiConstants.HEADER_CONTENT_DISPOSITION,
                        "attachment; filename=apis.zip").header(RestApiConstants.HEADER_CONTENT_TYPE,
                        APIConstants.APPLICATION_ZIP).build();
            } else {
                SynapseArtifactListDTO synapseArtifactListDTO = new SynapseArtifactListDTO();
                if (runtimeArtifactDto.getArtifact() instanceof List) {
                    synapseArtifactListDTO.setList((List<String>) runtimeArtifactDto.getArtifact());
                    synapseArtifactListDTO.setCount(((List<String>) runtimeArtifactDto.getArtifact()).size());
                }
                return Response.ok().entity(synapseArtifactListDTO)
                        .header(RestApiConstants.HEADER_CONTENT_TYPE, RestApiConstants.APPLICATION_JSON).build();
            }
        } else {
            return Response
                    .status(Response.Status.NOT_FOUND)
                    .entity(RestApiUtil.getErrorDTO(ExceptionCodes.NO_API_ARTIFACT_FOUND))
                    .build();
        }
    }
}
