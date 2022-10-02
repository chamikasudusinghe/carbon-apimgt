/*
 * Copyright (c) 2022, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.apimgt.rest.api.store.v1.common.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.wso2.carbon.apimgt.api.APIConsumer;
import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.apimgt.api.EmptyCallbackURLForCodeGrantsException;
import org.wso2.carbon.apimgt.api.ExceptionCodes;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.apimgt.api.model.APIKey;
import org.wso2.carbon.apimgt.api.model.AccessTokenInfo;
import org.wso2.carbon.apimgt.api.model.Application;
import org.wso2.carbon.apimgt.api.model.ApplicationConstants;
import org.wso2.carbon.apimgt.api.model.OAuthApplicationInfo;
import org.wso2.carbon.apimgt.api.model.Scope;
import org.wso2.carbon.apimgt.api.model.Subscriber;
import org.wso2.carbon.apimgt.impl.APIConstants;
import org.wso2.carbon.apimgt.impl.APIManagerFactory;
import org.wso2.carbon.apimgt.impl.dao.ApiMgtDAO;
import org.wso2.carbon.apimgt.impl.importexport.APIImportExportException;
import org.wso2.carbon.apimgt.impl.importexport.ExportFormat;
import org.wso2.carbon.apimgt.impl.importexport.ImportExportConstants;
import org.wso2.carbon.apimgt.impl.importexport.utils.CommonUtil;
import org.wso2.carbon.apimgt.impl.utils.APIUtil;
import org.wso2.carbon.apimgt.rest.api.common.RestApiCommonUtil;
import org.wso2.carbon.apimgt.rest.api.common.RestApiConstants;
import org.wso2.carbon.apimgt.rest.api.store.v1.common.mappings.APIInfoMappingUtil;
import org.wso2.carbon.apimgt.rest.api.store.v1.common.mappings.ApplicationKeyMappingUtil;
import org.wso2.carbon.apimgt.rest.api.store.v1.common.mappings.ApplicationMappingUtil;
import org.wso2.carbon.apimgt.rest.api.store.v1.common.models.ExportedApplication;
import org.wso2.carbon.apimgt.rest.api.store.v1.common.models.ExportedSubscribedAPI;
import org.wso2.carbon.apimgt.rest.api.store.v1.common.utils.ExportUtils;
import org.wso2.carbon.apimgt.rest.api.store.v1.common.utils.ImportUtils;
import org.wso2.carbon.apimgt.rest.api.store.v1.dto.APIInfoListDTO;
import org.wso2.carbon.apimgt.rest.api.store.v1.dto.APIKeyDTO;
import org.wso2.carbon.apimgt.rest.api.store.v1.dto.APIKeyGenerateRequestDTO;
import org.wso2.carbon.apimgt.rest.api.store.v1.dto.APIKeyRevokeRequestDTO;
import org.wso2.carbon.apimgt.rest.api.store.v1.dto.ApplicationDTO;
import org.wso2.carbon.apimgt.rest.api.store.v1.dto.ApplicationInfoDTO;
import org.wso2.carbon.apimgt.rest.api.store.v1.dto.ApplicationKeyDTO;
import org.wso2.carbon.apimgt.rest.api.store.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.carbon.apimgt.rest.api.store.v1.dto.ApplicationKeyListDTO;
import org.wso2.carbon.apimgt.rest.api.store.v1.dto.ApplicationKeyMappingRequestDTO;
import org.wso2.carbon.apimgt.rest.api.store.v1.dto.ApplicationListDTO;
import org.wso2.carbon.apimgt.rest.api.store.v1.dto.ApplicationTokenDTO;
import org.wso2.carbon.apimgt.rest.api.store.v1.dto.ApplicationTokenGenerateRequestDTO;
import org.wso2.carbon.apimgt.rest.api.store.v1.dto.PaginationDTO;
import org.wso2.carbon.apimgt.rest.api.store.v1.dto.ScopeInfoDTO;
import org.wso2.carbon.apimgt.rest.api.util.utils.RestAPIStoreUtils;
import org.wso2.carbon.apimgt.rest.api.util.utils.RestApiUtil;
import org.wso2.carbon.identity.oauth.config.OAuthServerConfiguration;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This is the service implementation class for api application api service operations
 */
public class ApplicationServiceImpl {

    private static final Log log = LogFactory.getLog(ApplicationServiceImpl.class);

    private ApplicationServiceImpl() {
    }

    /**
     * Retrieves all the applications that the user has access to
     *
     * @param groupId group Id
     * @param query   search condition
     * @param limit   max number of objects returns
     * @param offset  starting index
     * @return Response object containing resulted applications
     */
    public static ApplicationListDTO applicationsGet(String groupId, String query, String sortBy, String sortOrder,
            Integer limit, Integer offset, String organization) {

        limit = limit != null ? limit : RestApiConstants.PAGINATION_LIMIT_DEFAULT;
        offset = offset != null ? offset : RestApiConstants.PAGINATION_OFFSET_DEFAULT;
        sortOrder = sortOrder != null ? sortOrder : RestApiConstants.DEFAULT_SORT_ORDER;
        sortBy = sortBy != null ?
                ApplicationMappingUtil.getApplicationSortByField(sortBy) :
                APIConstants.APPLICATION_NAME;
        query = query == null ? "" : query;
        ApplicationListDTO applicationListDTO = new ApplicationListDTO();
        String username = RestApiCommonUtil.getLoggedInUsername();

        // todo: Do a second level filtering for the incoming group ID.
        // todo: eg: use case is when there are lots of applications which is accessible to his group "g1", he wants
        //  to see
        // todo: what are the applications shared to group "g2" among them.
        groupId = RestApiUtil.getLoggedInUserGroupId();
        try {
            APIConsumer apiConsumer = RestApiCommonUtil.getConsumer(username);
            Subscriber subscriber = new Subscriber(username);
            Application[] applications = apiConsumer.getApplicationsWithPagination(new Subscriber(username), groupId,
                    offset, limit, query, sortBy, sortOrder, organization);
            if (applications != null) {
                JSONArray applicationAttributesFromConfig = apiConsumer.getAppAttributesFromConfig(username);
                for (Application application : applications) {
                    // Remove hidden attributes and set the rest of the attributes from config
                    Map<String, String> existingApplicationAttributes = application.getApplicationAttributes();
                    Map<String, String> applicationAttributes = new HashMap<>();
                    if (existingApplicationAttributes != null && applicationAttributesFromConfig != null) {
                        for (Object object : applicationAttributesFromConfig) {
                            JSONObject attribute = (JSONObject) object;
                            Boolean hidden = (Boolean) attribute.get(APIConstants.ApplicationAttributes.HIDDEN);
                            String attributeName = (String) attribute.get(APIConstants.ApplicationAttributes.ATTRIBUTE);

                            if (!BooleanUtils.isTrue(hidden)) {
                                String attributeVal = existingApplicationAttributes.get(attributeName);
                                if (attributeVal != null) {
                                    applicationAttributes.put(attributeName, attributeVal);
                                } else {
                                    applicationAttributes.put(attributeName, "");
                                }
                            }
                        }
                    }
                    application.setApplicationAttributes(applicationAttributes);
                }
            }

            int applicationCount = getAllApplicationCount(subscriber, groupId, query);

            applicationListDTO = ApplicationMappingUtil.fromApplicationsToDTO(applications);
            ApplicationMappingUtil.setPaginationParamsWithSortParams(applicationListDTO, groupId, limit, offset,
                    applicationCount, sortOrder, sortBy.toLowerCase());

            return applicationListDTO;
        } catch (APIManagementException e) {
            if (RestApiUtil.rootCauseMessageMatches(e, "start index seems to be greater than the limit count")) {
                //this is not an error of the user as he does not know the total number of applications available.
                // Thus sends an empty response
                applicationListDTO.setCount(0);
                applicationListDTO.setPagination(new PaginationDTO());
                return applicationListDTO;
            } else {
                String errorMessage = "Error while retrieving Applications";
                RestApiUtil.handleInternalServerError(errorMessage, e, log);
            }
        }
        return null;
    }

    /**
     * @param fileInputStream
     * @return ExportedApplication
     * @throws APIManagementException APIManagementException
     */
    public static ExportedApplication getExportedApplication(InputStream fileInputStream)
            throws APIManagementException {

        String extractedFolderPath;
        try {
            extractedFolderPath = CommonUtil.getArchivePathOfExtractedDirectory(fileInputStream,
                    ImportExportConstants.UPLOAD_APPLICATION_FILE_NAME);
        } catch (APIImportExportException e) {
            throw new APIManagementException("Error while importing Application", e);
        }
        String jsonContent;
        try {
            jsonContent = ImportUtils.getApplicationDefinitionAsJson(extractedFolderPath);
        } catch (IOException e) {
            throw new APIManagementException("Error while reading the application definition", e);
        }

        // Retrieving the field "data" in api.yaml/json and convert it to a JSON object for further processing
        JsonElement configElement = new JsonParser().parse(jsonContent).getAsJsonObject().get(APIConstants.DATA);
        return new Gson().fromJson(configElement, ExportedApplication.class);
    }

    /**
     * @param ownerId
     * @param applicationDTO
     * @param organization
     * @return
     * @throws APIManagementException
     */
    public static Application preProcessApplication(String ownerId, ApplicationDTO applicationDTO, String organization,
            boolean isApplicationExist) throws APIManagementException {

        String username = RestApiCommonUtil.getLoggedInUsername();
        APIConsumer apiConsumer = RestApiCommonUtil.getConsumer(username);
        Application application;

        if (isApplicationExist) {
            int appId = APIUtil.getApplicationId(applicationDTO.getName(), ownerId);
            Application oldApplication = apiConsumer.getApplicationById(appId);
            application = preProcessAndUpdateApplication(ownerId, applicationDTO, oldApplication,
                    oldApplication.getUUID());
        } else {
            application = preProcessAndAddApplication(ownerId, applicationDTO, organization);
        }
        return application;
    }

    /**
     * @param applicationId
     * @param ownerId
     * @param applicationDTO
     * @param skipApplicationKeys
     * @param update
     * @param organization
     * @return
     * @throws APIManagementException
     */
    public static ApplicationInfoDTO applicationImport(int applicationId, String ownerId, ApplicationDTO applicationDTO,
            Boolean skipApplicationKeys, Boolean update) throws APIManagementException {

        String username = RestApiCommonUtil.getLoggedInUsername();
        APIConsumer apiConsumer = RestApiCommonUtil.getConsumer(username);
        Application importedApplication = apiConsumer.getApplicationById(applicationId);
        importedApplication.setOwner(ownerId);

        // check whether keys need to be skipped while import
        if (skipApplicationKeys == null || !skipApplicationKeys) {
            // if this is an update, old keys will be removed and the OAuth app will be overridden with new values
            if (update && applicationDTO.getKeys().size() > 0 && !importedApplication.getKeys().isEmpty()) {
                importedApplication.getKeys().clear();
            }

            // Add application keys if present and keys does not exist in the current application
            if (applicationDTO.getKeys().size() > 0 && importedApplication.getKeys().isEmpty()) {
                for (ApplicationKeyDTO applicationKeyDTO : applicationDTO.getKeys()) {
                    ImportUtils.addApplicationKey(ownerId, importedApplication, applicationKeyDTO, apiConsumer, update);
                }
            }
        }

        return ApplicationMappingUtil.fromApplicationToInfoDTO(importedApplication);
    }

    /**
     * Creates a new application
     *
     * @param body request body containing application details
     * @return 201 response if successful
     */
    public static ApplicationDTO addApplication(ApplicationDTO body, String organization)
            throws APIManagementException {

        ApplicationDTO createdApplicationDto = null;
        try {
            String username = RestApiCommonUtil.getLoggedInUsername();
            Application createdApplication = preProcessAndAddApplication(username, body, organization);
            createdApplicationDto = ApplicationMappingUtil.fromApplicationtoDTO(createdApplication);
        } catch (APIManagementException e) {
            if (RestApiUtil.isDueToResourceAlreadyExists(e)) {
                RestApiUtil.handleResourceAlreadyExistsError(
                        "An application already exists with name " + body.getName(), e, log);
            } else if (RestApiUtil.isDueToApplicationNameWhiteSpaceValidation(e)) {
                RestApiUtil.handleBadRequest("Application name cannot contain leading or trailing white spaces", log);
            } else if (RestApiUtil.isDueToApplicationNameWithInvalidCharacters(e)) {
                RestApiUtil.handleBadRequest("Application name cannot contain invalid characters", log);
            } else {
                throw e;
            }
        }
        return createdApplicationDto;
    }

    /**
     * Preprocess and add the application
     *
     * @param username       Username
     * @param applicationDto Application DTO
     * @param organization   Identifier of an organization
     * @return Created application
     */
    private static Application preProcessAndAddApplication(String username, ApplicationDTO applicationDto,
            String organization) throws APIManagementException {
        APIConsumer apiConsumer = APIManagerFactory.getInstance().getAPIConsumer(username);

        //validate the tier specified for the application
        String tierName = applicationDto.getThrottlingPolicy();
        if (tierName == null) {
            RestApiUtil.handleBadRequest("Throttling tier cannot be null", log);
        }

        Object applicationAttributesFromUser = applicationDto.getAttributes();
        Map<String, String> applicationAttributes = new ObjectMapper().convertValue(applicationAttributesFromUser,
                Map.class);
        if (applicationAttributes != null) {
            applicationDto.setAttributes(applicationAttributes);
        }

        //we do not honor tokenType sent in the body and
        //all the applications created will of 'JWT' token type
        applicationDto.setTokenType(ApplicationDTO.TokenTypeEnum.JWT);

        //subscriber field of the body is not honored. It is taken from the context
        Application application = ApplicationMappingUtil.fromDTOtoApplication(applicationDto, username);

        int applicationId = apiConsumer.addApplication(application, username, organization);

        //retrieves the created application and send as the response
        return apiConsumer.getApplicationById(applicationId);
    }

    /**
     * Get an application by Id
     *
     * @param applicationId application identifier
     * @return response containing the required application object
     */
    public static ApplicationDTO getApplicationById(String applicationId, String organization) {
        String username = RestApiCommonUtil.getLoggedInUsername();
        try {
            APIConsumer apiConsumer = APIManagerFactory.getInstance().getAPIConsumer(username);
            Application application = apiConsumer.getApplicationByUUID(applicationId, organization);
            if (application != null) {
                // Remove hidden attributes and set the rest of the attributes from config
                JSONArray applicationAttributesFromConfig = apiConsumer.getAppAttributesFromConfig(username);
                Map<String, String> existingApplicationAttributes = application.getApplicationAttributes();
                Map<String, String> applicationAttributes = processApplicationAttributesFromConfig(
                        existingApplicationAttributes, applicationAttributesFromConfig);
                application.setApplicationAttributes(applicationAttributes);
                if (RestAPIStoreUtils.isUserAccessAllowedForApplication(application)) {
                    ApplicationDTO applicationDTO = ApplicationMappingUtil.fromApplicationtoDTO(application);
                    applicationDTO.setHashEnabled(OAuthServerConfiguration.getInstance().isClientSecretHashEnabled());
                    Set<Scope> scopes = apiConsumer.getScopesForApplicationSubscription(username, application.getId(),
                            organization);
                    List<ScopeInfoDTO> scopeInfoList = ApplicationMappingUtil.getScopeInfoDTO(scopes);
                    applicationDTO.setSubscriptionScopes(scopeInfoList);
                    return applicationDTO;
                } else {
                    RestApiUtil.handleAuthorizationFailure(RestApiConstants.RESOURCE_APPLICATION, applicationId, log);
                }
            } else {
                RestApiUtil.handleResourceNotFoundError(RestApiConstants.RESOURCE_APPLICATION, applicationId, log);
            }
        } catch (APIManagementException e) {
            RestApiUtil.handleInternalServerError("Error while retrieving application " + applicationId, e, log);
        }
        return null;
    }

    /**
     * Update an application by Id
     *
     * @param applicationId application identifier
     * @param body          request body containing application details
     * @return response containing the updated application object
     */
    public static ApplicationDTO updateApplication(String applicationId, ApplicationDTO body)
            throws APIManagementException {
        String username = RestApiCommonUtil.getLoggedInUsername();
        try {
            APIConsumer apiConsumer = APIManagerFactory.getInstance().getAPIConsumer(username);
            Application oldApplication = apiConsumer.getApplicationByUUID(applicationId);

            if (oldApplication == null) {
                RestApiUtil.handleResourceNotFoundError(RestApiConstants.RESOURCE_APPLICATION, applicationId, log);
            }

            if (!RestAPIStoreUtils.isUserOwnerOfApplication(oldApplication)) {
                RestApiUtil.handleAuthorizationFailure(RestApiConstants.RESOURCE_APPLICATION, applicationId, log);
            }

            Application updatedApplication = preProcessAndUpdateApplication(username, body, oldApplication,
                    applicationId);
            return ApplicationMappingUtil.fromApplicationtoDTO(updatedApplication);

        } catch (APIManagementException e) {
            if (RestApiUtil.isDueToApplicationNameWhiteSpaceValidation(e)) {
                RestApiUtil.handleBadRequest("Application name cannot contains leading or trailing white spaces", log);
            } else if (RestApiUtil.isDueToApplicationNameWithInvalidCharacters(e)) {
                RestApiUtil.handleBadRequest("Application name cannot contain invalid characters", log);
            } else if (RestApiUtil.isDueToResourceAlreadyExists(e)) {
                RestApiUtil.handleResourceAlreadyExistsError(
                        "An application already exists with name " + body.getName(), e, log);
            } else {
                RestApiUtil.handleInternalServerError("Error while updating application " + applicationId, e, log);
            }
        }
        return null;
    }

    /**
     * Preprocess and update the application
     *
     * @param username       Username
     * @param applicationDto Application DTO
     * @param oldApplication Old application
     * @param applicationId  Application UUID
     * @return Updated application
     */
    private static Application preProcessAndUpdateApplication(String username, ApplicationDTO applicationDto,
            Application oldApplication, String applicationId) throws APIManagementException {
        APIConsumer apiConsumer = APIManagerFactory.getInstance().getAPIConsumer(username);
        Object applicationAttributesFromUser = applicationDto.getAttributes();
        Map<String, String> applicationAttributes = new ObjectMapper().convertValue(applicationAttributesFromUser,
                Map.class);

        if (applicationAttributes != null) {
            applicationDto.setAttributes(applicationAttributes);
        }

        //we do not honor the subscriber coming from the request body as we can't change the subscriber of the
        // application
        Application application = ApplicationMappingUtil.fromDTOtoApplication(applicationDto, username);

        //we do not honor the application id which is sent via the request body
        application.setUUID(oldApplication != null ? oldApplication.getUUID() : null);

        apiConsumer.updateApplication(application);

        //retrieves the updated application and send as the response
        return apiConsumer.getApplicationByUUID(applicationId);
    }

    /**
     * Export an existing Application
     *
     * @param appName  Search query
     * @param appOwner Owner of the Application
     * @param withKeys Export keys with application
     * @param format   Export format
     * @return Zip file containing exported Application
     */
    public static File exportApplication(String appName, String appOwner, Boolean withKeys, String format)
            throws APIManagementException {
        APIConsumer apiConsumer;
        Application application = null;

        if (StringUtils.isBlank(appName) || StringUtils.isBlank(appOwner)) {
            RestApiUtil.handleBadRequest("Application name or owner should not be empty or null.", log);
        }

        // Default export format is YAML
        ExportFormat exportFormat = StringUtils.isNotEmpty(format) ?
                ExportFormat.valueOf(format.toUpperCase()) :
                ExportFormat.YAML;

        String username = RestApiCommonUtil.getLoggedInUsername();
        apiConsumer = RestApiCommonUtil.getConsumer(username);

        if (appOwner != null && apiConsumer.getSubscriber(appOwner) != null) {
            application = ExportUtils.getApplicationDetails(appName, appOwner, apiConsumer);
        }
        if (application == null) {
            throw new APIManagementException("No application found with name " + appName + " owned by " + appOwner,
                    ExceptionCodes.APPLICATION_NOT_FOUND);
        } else if (!MultitenantUtils.getTenantDomain(application.getSubscriber().getName())
                .equals(MultitenantUtils.getTenantDomain(username))) {
            throw new APIManagementException("Cross Tenant Exports are not allowed", ExceptionCodes.TENANT_MISMATCH);
        }

        return ExportUtils.exportApplication(application, apiConsumer, exportFormat, withKeys);
    }

    /**
     * @param applicationId
     * @param keyType
     * @param body
     * @return
     * @throws APIManagementException
     */
    public static APIKeyDTO generateAPIKey(String applicationId, String keyType, APIKeyGenerateRequestDTO body)
            throws APIManagementException {

        APIKeyDTO apiKeyDto = null;
        String userName = RestApiCommonUtil.getLoggedInUsername();
        Application application;
        int validityPeriod;
        try {
            APIConsumer apiConsumer = APIManagerFactory.getInstance().getAPIConsumer(userName);
            if ((application = apiConsumer.getApplicationByUUID(applicationId)) == null) {
                throw new APIManagementException("Request application is " + (applicationId != null ?
                        "with id " + applicationId :
                        " " + "not found"), ExceptionCodes.APPLICATION_NOT_FOUND);
            } else {
                if (!RestAPIStoreUtils.isUserAccessAllowedForApplication(application)) {
                    RestApiUtil.handleAuthorizationFailure(RestApiConstants.RESOURCE_APPLICATION, applicationId, log);
                } else {
                    Object additionalProperties = null;
                    if (body != null && body.getAdditionalProperties() != null) {
                        additionalProperties = body.getAdditionalProperties();
                    }

                    if (body != null && body.getValidityPeriod() != null) {
                        validityPeriod = body.getValidityPeriod();
                    } else {
                        validityPeriod = -1;
                    }

                    String apiKey = generateApiKeyForApplication(apiConsumer, application, keyType, userName,
                            validityPeriod, additionalProperties);
                    apiKeyDto = ApplicationKeyMappingUtil.formApiKeyToDTO(apiKey, validityPeriod);
                }
            }
        } catch (APIManagementException e) {
            throw new APIManagementException(ExceptionCodes.from(ExceptionCodes.INTERNAL_ERROR_WITH_SPECIFIC_MESSAGE,
                    "Error while generating API Keys for application " + applicationId));
        }
        return apiKeyDto;
    }

    /**
     * @param applicationId applicationId
     * @param body          APIKeyRevokeRequest
     */
    public static void revokeAPIKey(String applicationId, APIKeyRevokeRequestDTO body) {
        String username = RestApiCommonUtil.getLoggedInUsername();
        String apiKey = body.getApikey();
        if (!StringUtils.isEmpty(apiKey) && APIUtil.isValidJWT(apiKey)) {
            try {
                String[] splitToken = apiKey.split("\\.");
                String signatureAlgorithm = APIUtil.getSignatureAlgorithm(splitToken);
                String certAlias = APIUtil.getSigningAlias(splitToken);
                Certificate certificate = APIUtil.getCertificateFromParentTrustStore(certAlias);
                if (APIUtil.verifyTokenSignature(splitToken, certificate, signatureAlgorithm)) {
                    APIConsumer apiConsumer = APIManagerFactory.getInstance().getAPIConsumer(username);
                    Application application = apiConsumer.getApplicationByUUID(applicationId);
                    org.json.JSONObject decodedBody = new org.json.JSONObject(
                            new String(Base64.getUrlDecoder().decode(splitToken[1])));
                    org.json.JSONObject appInfo = decodedBody.getJSONObject(APIConstants.JwtTokenConstants.APPLICATION);
                    if (appInfo != null && application != null) {
                        if (RestAPIStoreUtils.isUserOwnerOfApplication(application)) {
                            String appUuid = appInfo.getString(APIConstants.JwtTokenConstants.APPLICATION_UUID);
                            if (applicationId.equals(appUuid)) {
                                String tenantDomain = RestApiCommonUtil.getLoggedInUserTenantDomain();
                                revokeAPIKey(apiConsumer, splitToken[1], apiKey, tenantDomain);
                            } else {
                                if (log.isDebugEnabled()) {
                                    log.debug(
                                            "Application uuid " + applicationId + " isn't matched with the " +
                                                    "application in the token " + appUuid + " of API Key " +
                                                    APIUtil.getMaskedToken(
                                                    apiKey));
                                }
                                RestApiUtil.handleBadRequest("Validation failed for the given token ", log);
                            }
                        } else {
                            if (log.isDebugEnabled()) {
                                log.debug(
                                        "Logged in user " + username + " isn't the owner of the application "
                                                + applicationId);
                            }
                            RestApiUtil.handleAuthorizationFailure(RestApiConstants.RESOURCE_APPLICATION, applicationId,
                                    log);
                        }
                    } else {
                        if (log.isDebugEnabled()) {
                            if (application == null) {
                                log.debug("Application with given id " + applicationId + " doesn't not exist ");
                            }

                            if (appInfo == null) {
                                log.debug(
                                        "Application information doesn't exist in the token "
                                                + APIUtil.getMaskedToken(apiKey));
                            }
                        }
                        RestApiUtil.handleBadRequest("Validation failed for the given token ", log);
                    }
                } else {
                    if (log.isDebugEnabled()) {
                        log.debug("Signature verification of given token " + APIUtil.getMaskedToken(
                                apiKey) + " is failed");
                    }
                    RestApiUtil.handleInternalServerError("Validation failed for the given token", log);
                }
            } catch (APIManagementException e) {
                String msg = "Error while revoking API Key of application " + applicationId;
                if (log.isDebugEnabled()) {
                    log.debug(
                            "Error while revoking API Key of application " + applicationId + " and token "
                                    + APIUtil.getMaskedToken(apiKey));
                }
                log.error(msg, e);
                RestApiUtil.handleInternalServerError(msg, e, log);
            }
        } else {
            log.debug("Provided API Key " + APIUtil.getMaskedToken(apiKey) + " is not valid");
            RestApiUtil.handleBadRequest("Provided API Key isn't valid ", log);
        }
    }

    /**
     * Deletes an application by id
     *
     * @param applicationId application identifier
     * @return 200 Response if successfully deleted the application
     */
    public static int deleteApplication(String applicationId) {
        String username = RestApiCommonUtil.getLoggedInUsername();
        try {
            APIConsumer apiConsumer = APIManagerFactory.getInstance().getAPIConsumer(username);
            Application application = apiConsumer.getLightweightApplicationByUUID(applicationId);
            if (application != null) {
                if (RestAPIStoreUtils.isUserOwnerOfApplication(application)) {
                    apiConsumer.removeApplication(application, username);
                    if (APIConstants.ApplicationStatus.DELETE_PENDING.equals(application.getStatus())) {
                        return application.getId();
                    }
                } else {
                    RestApiUtil.handleAuthorizationFailure(RestApiConstants.RESOURCE_APPLICATION, applicationId, log);
                }
            } else {
                RestApiUtil.handleResourceNotFoundError(RestApiConstants.RESOURCE_APPLICATION, applicationId, log);
            }
        } catch (APIManagementException e) {
            RestApiUtil.handleInternalServerError("Error while deleting application " + applicationId, e, log);
        }
        return 0;
    }

    /**
     * Generate keys for a application
     *
     * @param applicationId application identifier
     * @param body          request body
     * @return A response object containing application keys
     */
    public static ApplicationKeyDTO generateKeys(String applicationId, ApplicationKeyGenerateRequestDTO body,
            String organization) throws APIManagementException {

        String username = RestApiCommonUtil.getLoggedInUsername();
        try {
            APIConsumer apiConsumer = APIManagerFactory.getInstance().getAPIConsumer(username);
            Application application = apiConsumer.getApplicationByUUID(applicationId);
            if (application != null) {
                if (RestAPIStoreUtils.isUserOwnerOfApplication(application)) {
                    String keyManagerName = APIConstants.KeyManager.DEFAULT_KEY_MANAGER;
                    if (StringUtils.isNotEmpty(body.getKeyManager())) {
                        keyManagerName = body.getKeyManager();
                    }
                    Map<String, Object> keyDetails = requestApprovalForApplicationRegistration(apiConsumer, body,
                            application, username, keyManagerName, organization);

                    ApplicationKeyDTO applicationKeyDTO = ApplicationKeyMappingUtil.fromApplicationKeyToDTO(keyDetails,
                            body.getKeyType().toString());
                    applicationKeyDTO.setKeyManager(keyManagerName);
                    return applicationKeyDTO;
                } else {
                    RestApiUtil.handleAuthorizationFailure(RestApiConstants.RESOURCE_APPLICATION, applicationId, log);
                }
            } else {
                RestApiUtil.handleResourceNotFoundError(RestApiConstants.RESOURCE_APPLICATION, applicationId, log);
            }
        } catch (EmptyCallbackURLForCodeGrantsException e) {
            RestApiUtil.handleBadRequest(e.getMessage(), log);
        }
        return null;
    }

    /**
     * Retrieve all keys of an application
     *
     * @param applicationId Application Id
     * @return Application Key Information list
     */
    public static ApplicationKeyListDTO getApplicationKeysByApplicationId(String applicationId) {

        Set<APIKey> applicationKeys = getApplicationKeys(applicationId);
        List<ApplicationKeyDTO> keyDTOList = new ArrayList<>();
        ApplicationKeyListDTO applicationKeyListDTO = new ApplicationKeyListDTO();
        applicationKeyListDTO.setCount(0);

        if (applicationKeys != null) {
            for (APIKey apiKey : applicationKeys) {
                ApplicationKeyDTO appKeyDTO = ApplicationKeyMappingUtil.fromApplicationKeyToDTO(apiKey);
                keyDTOList.add(appKeyDTO);
            }
            applicationKeyListDTO.setList(keyDTOList);
            applicationKeyListDTO.setCount(keyDTOList.size());
        }
        return applicationKeyListDTO;
    }

    /**
     * Clean up application keys
     *
     * @param applicationId Application Id
     * @param keyType       Key Type whether PRODUCTION or SANDBOX
     * @return
     */
    public static void cleanupApplicationRegistration(String applicationId, String keyType) {

        String username = RestApiCommonUtil.getLoggedInUsername();
        try {
            APIConsumer apiConsumer = APIManagerFactory.getInstance().getAPIConsumer(username);
            Application application = apiConsumer.getLightweightApplicationByUUID(applicationId);
            apiConsumer.cleanUpApplicationRegistrationByApplicationId(application.getId(), keyType);
        } catch (APIManagementException e) {
            RestApiUtil.handleInternalServerError("Error occurred while application key cleanup process", e, log);
        }
    }

    /**
     * Used to get all keys of an application
     *
     * @param applicationUUID Id of the application
     * @return List of application keys
     */
    private static Set<APIKey> getApplicationKeys(String applicationUUID, String tenantDomain) {

        String username = RestApiCommonUtil.getLoggedInUsername();
        try {
            APIConsumer apiConsumer = APIManagerFactory.getInstance().getAPIConsumer(username);
            Application application = apiConsumer.getLightweightApplicationByUUID(applicationUUID);
            if (application != null) {
                if (RestAPIStoreUtils.isUserAccessAllowedForApplication(application)) {
                    return apiConsumer.getApplicationKeysOfApplication(application.getId(), tenantDomain);
                } else {
                    RestApiUtil.handleAuthorizationFailure(RestApiConstants.RESOURCE_APPLICATION, applicationUUID, log);
                }
            } else {
                RestApiUtil.handleResourceNotFoundError(RestApiConstants.RESOURCE_APPLICATION, applicationUUID, log);
            }
        } catch (APIManagementException e) {
            RestApiUtil.handleInternalServerError("Error while retrieving application " + applicationUUID, e, log);
        }
        return null;
    }

    /**
     * Used to get all keys of an application
     *
     * @param applicationUUID Id of the application
     * @return List of application keys
     */
    private static Set<APIKey> getApplicationKeys(String applicationUUID) {

        return getApplicationKeys(applicationUUID, null);
    }

    public static ApplicationTokenDTO generateToken(String applicationId, String keyType,
            ApplicationTokenGenerateRequestDTO body) {
        try {
            String username = RestApiCommonUtil.getLoggedInUsername();
            APIConsumer apiConsumer = RestApiCommonUtil.getConsumer(username);
            Application application = apiConsumer.getApplicationByUUID(applicationId);

            if (application != null) {
                if (RestAPIStoreUtils.isUserAccessAllowedForApplication(application)) {
                    ApplicationKeyDTO appKey = getApplicationKeyByAppIDAndKeyType(applicationId, keyType);
                    if (appKey != null) {
                        String jsonInput = null;
                        String grantType;
                        if (ApplicationTokenGenerateRequestDTO.GrantTypeEnum.TOKEN_EXCHANGE.equals(
                                body.getGrantType())) {
                            grantType = APIConstants.OAuthConstants.TOKEN_EXCHANGE;
                        } else {
                            grantType = APIConstants.GRANT_TYPE_CLIENT_CREDENTIALS;
                        }

                        if (StringUtils.isNotEmpty(body.getConsumerSecret())) {
                            appKey.setConsumerSecret(body.getConsumerSecret());
                        }
                        String[] scopes = body.getScopes().toArray(new String[0]);
                        AccessTokenInfo response = apiConsumer.renewAccessToken(body.getRevokeToken(),
                                appKey.getConsumerKey(), appKey.getConsumerSecret(),
                                body.getValidityPeriod().toString(), scopes, jsonInput,
                                APIConstants.KeyManager.DEFAULT_KEY_MANAGER, grantType);

                        ApplicationTokenDTO appToken = new ApplicationTokenDTO();
                        appToken.setAccessToken(response.getAccessToken());
                        appToken.setTokenScopes(Arrays.asList(response.getScopes()));
                        appToken.setValidityTime(response.getValidityPeriod());
                        return appToken;
                    } else {
                        RestApiUtil.handleResourceNotFoundError(RestApiConstants.RESOURCE_APP_CONSUMER_KEY, keyType,
                                log);
                    }
                } else {
                    RestApiUtil.handleAuthorizationFailure(RestApiConstants.RESOURCE_APPLICATION, applicationId, log);
                }
            } else {
                RestApiUtil.handleResourceNotFoundError(RestApiConstants.RESOURCE_APPLICATION, applicationId, log);
            }
        } catch (APIManagementException e) {
            RestApiUtil.handleInternalServerError(
                    RestApiConstants.GENERIC_ERROR_STRING + " " + keyType + " token for application "
                            + applicationId,
                    e, log);
        }
        return null;
    }

    /**
     * Returns Keys of an application by key type
     *
     * @param applicationId Application Id
     * @param keyType       Key Type (Production | Sandbox)
     * @return Application Key Information
     */
    public static ApplicationKeyDTO getApplicationKeyByAppIDAndKeyType(String applicationId, String keyType) {
        Set<APIKey> applicationKeys = getApplicationKeys(applicationId);
        if (applicationKeys != null) {
            for (APIKey apiKey : applicationKeys) {
                if (keyType != null && keyType.equals(
                        apiKey.getType()) && APIConstants.KeyManager.DEFAULT_KEY_MANAGER.equals(
                        apiKey.getKeyManager())) {
                    return ApplicationKeyMappingUtil.fromApplicationKeyToDTO(apiKey);
                }
            }
        }
        return null;
    }

    /**
     * Returns Keys of an application by key type
     *
     * @param applicationId Application Id
     * @param keyMappingId  Key Mapping ID
     * @return Application Key Information
     */
    public static ApplicationKeyDTO getApplicationKeyByAppIDAndKeyMapping(String applicationId, String keyMappingId) {
        String username = RestApiCommonUtil.getLoggedInUsername();
        try {
            APIConsumer apiConsumer = APIManagerFactory.getInstance().getAPIConsumer(username);
            Application application = apiConsumer.getLightweightApplicationByUUID(applicationId);
            if (application != null) {
                APIKey apiKey = apiConsumer.getApplicationKeyByAppIDAndKeyMapping(application.getId(), keyMappingId);
                if (apiKey != null) {
                    return ApplicationKeyMappingUtil.fromApplicationKeyToDTO(apiKey);
                }
            } else {
                log.error("Application not found with ID: " + applicationId);
            }
        } catch (APIManagementException e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    /**
     * Update grant types/callback URL
     *
     * @param applicationId Application Id
     * @param keyType       Key Type (Production | Sandbox)
     * @param body          Grant type and callback URL information
     * @return Updated Key Information
     */
    public static ApplicationKeyDTO updateApplicationKeysKeyType(String applicationId, String keyType,
            ApplicationKeyDTO body) {
        String username = RestApiCommonUtil.getLoggedInUsername();
        try {
            APIConsumer apiConsumer = APIManagerFactory.getInstance().getAPIConsumer(username);
            Application application = apiConsumer.getApplicationByUUID(applicationId);
            if (application != null) {
                if (RestAPIStoreUtils.isUserOwnerOfApplication(application)) {
                    OAuthApplicationInfo updatedData = updateAuthClient(apiConsumer, body, username, application,
                            keyType);
                    ApplicationKeyDTO applicationKeyDTO = new ApplicationKeyDTO();
                    applicationKeyDTO.setCallbackUrl(updatedData.getCallBackURL());
                    JsonObject json = new Gson().fromJson(updatedData.getJsonString(), JsonObject.class);
                    if (json.get(APIConstants.JSON_GRANT_TYPES) != null) {
                        String[] updatedGrantTypes = json.get(APIConstants.JSON_GRANT_TYPES)
                                .getAsString().split(" ");
                        applicationKeyDTO.setSupportedGrantTypes(Arrays.asList(updatedGrantTypes));
                    }
                    applicationKeyDTO.setConsumerKey(updatedData.getClientId());
                    applicationKeyDTO.setConsumerSecret(updatedData.getClientSecret());
                    applicationKeyDTO.setKeyType(ApplicationKeyDTO.KeyTypeEnum.valueOf(keyType));
                    Object additionalProperties = updatedData.getParameter(APIConstants.JSON_ADDITIONAL_PROPERTIES);
                    if (additionalProperties != null) {
                        applicationKeyDTO.setAdditionalProperties(additionalProperties);
                    }
                    return applicationKeyDTO;
                } else {
                    RestApiUtil.handleAuthorizationFailure(RestApiConstants.RESOURCE_APPLICATION, applicationId, log);
                }
            } else {
                RestApiUtil.handleResourceNotFoundError(RestApiConstants.RESOURCE_APPLICATION, applicationId, log);
            }
        } catch (APIManagementException e) {
            RestApiUtil.handleInternalServerError("Error while updating application " + applicationId, e, log);
        }
        return null;
    }

    /**
     * Re generate consumer secret.
     *
     * @param applicationId Application Id
     * @param keyType       Key Type (Production | Sandbox)
     * @return A response object containing application keys.
     */
    public static ApplicationKeyDTO renewConsumerSecret(String applicationId, String keyType) {
        String username = RestApiCommonUtil.getLoggedInUsername();
        try {
            Set<APIKey> applicationKeys = getApplicationKeys(applicationId);
            if (applicationKeys == null) {
                return null;
            }
            for (APIKey apiKey : applicationKeys) {
                if (keyType != null && keyType.equals(
                        apiKey.getType()) && APIConstants.KeyManager.DEFAULT_KEY_MANAGER.equals(
                        apiKey.getKeyManager())) {
                    APIConsumer apiConsumer = APIManagerFactory.getInstance().getAPIConsumer(username);
                    String clientId = apiKey.getConsumerKey();
                    String clientSecret = apiConsumer.renewConsumerSecret(clientId,
                            APIConstants.KeyManager.DEFAULT_KEY_MANAGER);

                    ApplicationKeyDTO applicationKeyDTO = new ApplicationKeyDTO();
                    applicationKeyDTO.setConsumerKey(clientId);
                    applicationKeyDTO.setConsumerSecret(clientSecret);

                    return applicationKeyDTO;
                }
            }

        } catch (APIManagementException e) {
            RestApiUtil.handleInternalServerError("Error while re generating the consumer secret ", e, log);
        }
        return null;
    }

    /**
     * Generate keys using existing consumer key and consumer secret
     *
     * @param applicationId Application id
     * @param body          Contains consumer key, secret and key type information
     * @return A response object containing application keys
     */
    public static ApplicationKeyDTO mapApplicationKeys(String applicationId, ApplicationKeyMappingRequestDTO body,
            String organization) throws APIManagementException {

        String username = RestApiCommonUtil.getLoggedInUsername();
        JSONObject jsonParamObj = new JSONObject();
        APIConsumer apiConsumer = APIManagerFactory.getInstance().getAPIConsumer(username);
        Application application = apiConsumer.getApplicationByUUID(applicationId);
        String keyManagerName = APIConstants.KeyManager.DEFAULT_KEY_MANAGER;
        if (StringUtils.isNotEmpty(body.getKeyManager())) {
            keyManagerName = body.getKeyManager();
        }
        if (application != null) {
            if (RestAPIStoreUtils.isUserOwnerOfApplication(application)) {
                String clientId = body.getConsumerKey();
                String keyType = body.getKeyType().toString();
                String tokenType = APIConstants.DEFAULT_TOKEN_TYPE;
                jsonParamObj.put(APIConstants.SUBSCRIPTION_KEY_TYPE, body.getKeyType().toString());
                jsonParamObj.put(APIConstants.JSON_CLIENT_SECRET, body.getConsumerSecret());
                Map<String, Object> keyDetails = apiConsumer.mapExistingOAuthClient(jsonParamObj.toJSONString(),
                        username, clientId, application.getName(), keyType, tokenType, keyManagerName, organization);
                ApplicationKeyDTO applicationKeyDTO = ApplicationKeyMappingUtil.fromApplicationKeyToDTO(keyDetails,
                        body.getKeyType().toString());
                applicationKeyDTO.setKeyManager(keyManagerName);
                return applicationKeyDTO;
            } else {
                RestApiUtil.handleAuthorizationFailure(RestApiConstants.RESOURCE_APPLICATION, applicationId, log);
            }
        } else {
            RestApiUtil.handleResourceNotFoundError(RestApiConstants.RESOURCE_APPLICATION, applicationId, log);
        }
        return null;
    }

    /**
     * @param applicationId
     * @param organization
     * @return
     * @throws APIManagementException
     */
    public static ApplicationKeyListDTO getApplicationIdOauthKeys(String applicationId, String organization)
            throws APIManagementException {
        Set<APIKey> applicationKeys = getApplicationKeys(applicationId, organization);
        List<ApplicationKeyDTO> keyDTOList = new ArrayList<>();
        ApplicationKeyListDTO applicationKeyListDTO = new ApplicationKeyListDTO();
        applicationKeyListDTO.setCount(0);

        if (applicationKeys != null) {
            for (APIKey apiKey : applicationKeys) {
                ApplicationKeyDTO appKeyDTO = ApplicationKeyMappingUtil.fromApplicationKeyToDTO(apiKey);
                keyDTOList.add(appKeyDTO);
            }
            applicationKeyListDTO.setList(keyDTOList);
            applicationKeyListDTO.setCount(keyDTOList.size());
        }
        return applicationKeyListDTO;
    }

    /**
     * @param applicationId
     * @param keyMappingId
     * @throws APIManagementException
     */
    public static void cleanUpApplicationRegistrationByApplicationIdAndKeyMappingId(String applicationId,
            String keyMappingId) throws APIManagementException {

        String username = RestApiCommonUtil.getLoggedInUsername();
        try {
            APIConsumer apiConsumer = APIManagerFactory.getInstance().getAPIConsumer(username);
            Application application = apiConsumer.getLightweightApplicationByUUID(applicationId);
            apiConsumer.cleanUpApplicationRegistrationByApplicationIdAndKeyMappingId(application.getId(), keyMappingId);
        } catch (APIManagementException e) {
            RestApiUtil.handleInternalServerError("Error occurred while application key cleanup process", e, log);
        }
    }

    /***
     *
     * @param applicationId
     * @param keyMappingId
     * @param body
     * @return
     * @throws APIManagementException
     */
    public static ApplicationTokenDTO generateTokenByOauthKeysKeyMappingId(String applicationId, String keyMappingId,
            ApplicationTokenGenerateRequestDTO body) throws APIManagementException {

        String username = RestApiCommonUtil.getLoggedInUsername();
        APIConsumer apiConsumer = RestApiCommonUtil.getConsumer(username);
        Application application = apiConsumer.getApplicationByUUID(applicationId);

        if (application != null) {
            if (RestAPIStoreUtils.isUserAccessAllowedForApplication(application)) {
                ApplicationKeyDTO appKey = getApplicationKeyByAppIDAndKeyMapping(applicationId, keyMappingId);
                if (appKey != null) {
                    String jsonInput = null;
                    String grantType;
                    if (ApplicationTokenGenerateRequestDTO.GrantTypeEnum.TOKEN_EXCHANGE.equals(body.getGrantType())) {
                        grantType = APIConstants.OAuthConstants.TOKEN_EXCHANGE;
                    } else {
                        grantType = APIConstants.GRANT_TYPE_CLIENT_CREDENTIALS;
                    }

                    jsonInput = validateAdditionalParameters(grantType, body, appKey.getKeyType(), applicationId);

                    if (StringUtils.isNotEmpty(body.getConsumerSecret())) {
                        appKey.setConsumerSecret(body.getConsumerSecret());
                    }
                    String[] scopes = body.getScopes().toArray(new String[0]);

                    try {
                        AccessTokenInfo response = apiConsumer.renewAccessToken(body.getRevokeToken(),
                                appKey.getConsumerKey(), appKey.getConsumerSecret(),
                                body.getValidityPeriod().toString(), scopes, jsonInput, appKey.getKeyManager(),
                                grantType);
                        ApplicationTokenDTO appToken = new ApplicationTokenDTO();
                        appToken.setAccessToken(response.getAccessToken());
                        if (response.getScopes() != null) {
                            appToken.setTokenScopes(Arrays.asList(response.getScopes()));
                        }
                        appToken.setValidityTime(response.getValidityPeriod());
                        return appToken;
                    } catch (APIManagementException e) {
                        Long errorCode = e.getErrorHandler() != null ?
                                e.getErrorHandler().getErrorCode() :
                                ExceptionCodes.INTERNAL_ERROR.getErrorCode();
                        RestApiUtil.handleBadRequest(e.getMessage(), errorCode, log);
                    }
                } else {
                    RestApiUtil.handleResourceNotFoundError(RestApiConstants.RESOURCE_APP_CONSUMER_KEY, keyMappingId,
                            log);
                }
            } else {
                RestApiUtil.handleAuthorizationFailure(RestApiConstants.RESOURCE_APPLICATION, applicationId, log);
            }
        } else {
            RestApiUtil.handleResourceNotFoundError(RestApiConstants.RESOURCE_APPLICATION, applicationId, log);
        }
        return null;

    }

    /**
     * @param applicationId
     * @param keyMappingId
     * @param body
     * @return
     * @throws APIManagementException
     */
    public static ApplicationKeyDTO applicationsApplicationIdOauthKeysKeyMappingIdPut(String applicationId,
            String keyMappingId, ApplicationKeyDTO body) throws APIManagementException {

        String username = RestApiCommonUtil.getLoggedInUsername();
        APIConsumer apiConsumer = APIManagerFactory.getInstance().getAPIConsumer(username);
        Application application = apiConsumer.getApplicationByUUID(applicationId);
        if (application != null) {
            ApplicationKeyDTO appKey = getApplicationKeyByAppIDAndKeyMapping(applicationId, keyMappingId);
            if (RestAPIStoreUtils.isUserOwnerOfApplication(application) && appKey != null) {
                String grantTypes = StringUtils.join(body.getSupportedGrantTypes(), ',');
                JsonObject jsonParams = new JsonObject();
                jsonParams.addProperty(APIConstants.JSON_GRANT_TYPES, grantTypes);
                jsonParams.addProperty(APIConstants.JSON_USERNAME, username);
                if (body.getAdditionalProperties() != null) {
                    if (body.getAdditionalProperties() instanceof String && StringUtils.isNotEmpty(
                            (String) body.getAdditionalProperties())) {
                        jsonParams.addProperty(APIConstants.JSON_ADDITIONAL_PROPERTIES,
                                (String) body.getAdditionalProperties());
                    } else if (body.getAdditionalProperties() instanceof Map) {
                        String jsonContent = new Gson().toJson(body.getAdditionalProperties());
                        jsonParams.addProperty(APIConstants.JSON_ADDITIONAL_PROPERTIES, jsonContent);
                    }
                }
                OAuthApplicationInfo updatedData = apiConsumer.updateAuthClient(username, application,
                        appKey.getKeyType().value(), body.getCallbackUrl(), null, null,
                        null, body.getGroupId(), new Gson().toJson(jsonParams), appKey.getKeyManager());
                ApplicationKeyDTO applicationKeyDTO = new ApplicationKeyDTO();
                applicationKeyDTO.setCallbackUrl(updatedData.getCallBackURL());
                JsonObject json = new Gson().fromJson(updatedData.getJsonString(), JsonObject.class);
                if (json.get(APIConstants.JSON_GRANT_TYPES) != null) {
                    String[] updatedGrantTypes = json.get(APIConstants.JSON_GRANT_TYPES).getAsString().split(" ");
                    applicationKeyDTO.setSupportedGrantTypes(Arrays.asList(updatedGrantTypes));
                }
                applicationKeyDTO.setConsumerKey(updatedData.getClientId());
                applicationKeyDTO.setConsumerSecret(updatedData.getClientSecret());
                applicationKeyDTO.setKeyType(appKey.getKeyType());
                Object additionalProperties = updatedData.getParameter(APIConstants.JSON_ADDITIONAL_PROPERTIES);
                if (additionalProperties != null) {
                    applicationKeyDTO.setAdditionalProperties(additionalProperties);
                }
                applicationKeyDTO.setKeyMappingId(body.getKeyMappingId());
                applicationKeyDTO.setKeyManager(body.getKeyManager());
                return applicationKeyDTO;
            } else {
                RestApiUtil.handleAuthorizationFailure(RestApiConstants.RESOURCE_APPLICATION, applicationId, log);
            }
        } else {
            RestApiUtil.handleResourceNotFoundError(RestApiConstants.RESOURCE_APPLICATION, applicationId, log);
        }
        return null;
    }

    /**
     * @param applicationId
     * @param keyMappingId
     * @return
     * @throws APIManagementException
     */
    public static ApplicationKeyDTO applicationsApplicationIdOauthKeysKeyMappingIdRegenerateSecretPost(
            String applicationId, String keyMappingId) throws APIManagementException {

        String username = RestApiCommonUtil.getLoggedInUsername();
        Set<APIKey> applicationKeys = getApplicationKeys(applicationId);
        if (applicationKeys == null) {
            return null;
        }
        ApplicationKeyDTO applicationKeyDTO = getApplicationKeyByAppIDAndKeyMapping(applicationId, keyMappingId);
        if (applicationKeyDTO != null) {
            APIConsumer apiConsumer = APIManagerFactory.getInstance().getAPIConsumer(username);
            String clientId = applicationKeyDTO.getConsumerKey();
            String clientSecret = apiConsumer.renewConsumerSecret(clientId, applicationKeyDTO.getKeyManager());

            ApplicationKeyDTO retrievedApplicationKey = new ApplicationKeyDTO();
            retrievedApplicationKey.setConsumerKey(clientId);
            retrievedApplicationKey.setConsumerSecret(clientSecret);

            return retrievedApplicationKey;
        }
        return null;
    }

    private static String validateAdditionalParameters(String grantType, ApplicationTokenGenerateRequestDTO body,
            ApplicationKeyDTO.KeyTypeEnum keyType, String applicationId) {

        String jsonInput = null;
        try {
            // verify that the provided jsonInput is a valid json
            if (body.getAdditionalProperties() != null && !body.getAdditionalProperties().toString().isEmpty()) {
                ObjectMapper mapper = new ObjectMapper();
                jsonInput = mapper.writeValueAsString(body.getAdditionalProperties());
                JSONParser parser = new JSONParser();
                JSONObject json = (JSONObject) parser.parse(jsonInput);
                if (APIConstants.OAuthConstants.TOKEN_EXCHANGE.equals(grantType) && json.get(
                        APIConstants.OAuthConstants.SUBJECT_TOKEN) == null) {
                    RestApiUtil.handleBadRequest(
                            "Missing required parameter " + APIConstants.OAuthConstants.SUBJECT_TOKEN +
                                    " is not provided to generate token using Token Exchange grant",
                            log);
                }
            }
        } catch (JsonProcessingException | ParseException | ClassCastException e) {
            RestApiUtil.handleBadRequest(
                    RestApiConstants.GENERIC_ERROR_STRING + " " + keyType + " token for " + "application "
                            + applicationId + ". Invalid jsonInput '" + body.getAdditionalProperties() + "' provided.",
                    log);
        }

        return jsonInput;
    }

    /**
     * @param subscriber Subscriber
     * @param groupId    GroupId
     * @param query      Query
     * @return Integer
     * @throws APIManagementException APIManagementException
     */
    private static Integer getAllApplicationCount(Subscriber subscriber, String groupId, String query)
            throws APIManagementException {
        ApiMgtDAO apiMgtDAO = ApiMgtDAO.getInstance();
        return apiMgtDAO.getAllApplicationCount(subscriber, groupId, query);
    }

    /**
     * @param applicationGroupId
     * @param appOwner
     * @param preserveOwner
     * @param exportedAppOwner
     * @return
     * @throws APIManagementException
     */
    public static String getOwnerId(List<String> groups, String appOwner, Boolean preserveOwner,
            String exportedAppOwner) throws APIManagementException {

        String username = RestApiCommonUtil.getLoggedInUsername();
        APIConsumer apiConsumer = RestApiCommonUtil.getConsumer(username);
        String ownerId;
        if (!StringUtils.isBlank(appOwner)) {
            ownerId = appOwner;
        } else if (preserveOwner != null && preserveOwner) {
            ownerId = exportedAppOwner;
        } else {
            ownerId = username;
        }

        if (!MultitenantUtils.getTenantDomain(ownerId).equals(MultitenantUtils.getTenantDomain(username))) {
            throw new APIManagementException("Cross Tenant Imports are not allowed", ExceptionCodes.TENANT_MISMATCH);
        }

        String applicationGroupId = String.join(",", groups);

        if (groups != null && groups.size() > 0) {
            ImportUtils.validateOwner(username, applicationGroupId, apiConsumer);
        }

        return ownerId;
    }

    public static APIInfoListDTO getSkippedAPIs(Set<ExportedSubscribedAPI> exportedSubscribedAPIs, String ownerId,
            Boolean update, Boolean skipSubscriptions, Application application, String organization)
            throws APIManagementException {

        String username = RestApiCommonUtil.getLoggedInUsername();
        APIConsumer apiConsumer = RestApiCommonUtil.getConsumer(username);

        List<APIIdentifier> skippedAPIs = new ArrayList<>();
        if (skipSubscriptions == null || !skipSubscriptions) {
            skippedAPIs = ImportUtils.importSubscriptions(exportedSubscribedAPIs, ownerId, application, update,
                    apiConsumer, organization);
        }

        if (skippedAPIs.isEmpty()) {
            return null;
        } else {
            try {
                return APIInfoMappingUtil.fromAPIInfoListToDTO(skippedAPIs);
            } catch (UnsupportedEncodingException e) {
                throw new APIManagementException("Error while Decoding apiId", e);
            }
        }
    }

    private static Map<String, String> processApplicationAttributesFromConfig(
            Map<String, String> existingApplicationAttributes, JSONArray applicationAttributesFromConfig) {
        Map<String, String> applicationAttributes = new HashMap<>();
        if (existingApplicationAttributes != null && applicationAttributesFromConfig != null) {
            for (Object object : applicationAttributesFromConfig) {
                JSONObject attribute = (JSONObject) object;
                Boolean hidden = (Boolean) attribute.get(APIConstants.ApplicationAttributes.HIDDEN);
                String attributeName = (String) attribute.get(APIConstants.ApplicationAttributes.ATTRIBUTE);

                if (!BooleanUtils.isTrue(hidden)) {
                    String attributeVal = existingApplicationAttributes.get(attributeName);
                    if (attributeVal != null) {
                        applicationAttributes.put(attributeName, attributeVal);
                    } else {
                        applicationAttributes.put(attributeName, "");
                    }
                }
            }
        }
        return applicationAttributes;
    }

    private static String generateApiKeyForApplication(APIConsumer apiConsumer, Application application, String keyType,
            String username, Integer validityPeriod, Object additionalAttribute) throws APIManagementException {

        if (APIConstants.API_KEY_TYPE_PRODUCTION.equalsIgnoreCase(keyType)) {
            application.setKeyType(APIConstants.API_KEY_TYPE_PRODUCTION);
        } else if (APIConstants.API_KEY_TYPE_SANDBOX.equalsIgnoreCase(keyType)) {
            application.setKeyType(APIConstants.API_KEY_TYPE_SANDBOX);
        } else {
            String errorMessage = "Invalid keyType.";
            throw new APIManagementException(errorMessage, ExceptionCodes.from(ExceptionCodes.INVALID_KEY_TYPE));
        }

        if (validityPeriod == null) {
            validityPeriod = -1;
        }

        String restrictedIP = null;
        String restrictedReferer = null;

        if (additionalAttribute != null) {
            HashMap additionalProperties = (HashMap) additionalAttribute;
            if (additionalProperties.get(APIConstants.JwtTokenConstants.PERMITTED_IP) != null) {
                restrictedIP = (String) additionalProperties.get(APIConstants.JwtTokenConstants.PERMITTED_IP);
            }
            if (additionalProperties.get(APIConstants.JwtTokenConstants.PERMITTED_REFERER) != null) {
                restrictedReferer = (String) additionalProperties.get(APIConstants.JwtTokenConstants.PERMITTED_REFERER);
            }
        }
        return apiConsumer.generateApiKey(application, username, validityPeriod, restrictedIP, restrictedReferer);
    }

    private static void revokeAPIKey(APIConsumer apiConsumer, String tokenPayload, String apiKey, String tenantDomain)
            throws APIManagementException {
        long expiryTime = Long.MAX_VALUE;
        org.json.JSONObject payload = new org.json.JSONObject(new String(Base64.getUrlDecoder().decode(tokenPayload)));
        if (payload.has(APIConstants.JwtTokenConstants.EXPIRY_TIME)) {
            expiryTime = APIUtil.getExpiryifJWT(apiKey);
        }
        String tokenIdentifier = payload.getString(APIConstants.JwtTokenConstants.JWT_ID);
        apiConsumer.revokeAPIKey(tokenIdentifier, expiryTime, tenantDomain);
    }

    private static Map<String, Object> requestApprovalForApplicationRegistration(APIConsumer apiConsumer,
            ApplicationKeyGenerateRequestDTO body, Application application, String username, String keyManagerName,
            String organization) throws APIManagementException {

        String clientId = body.getClientId();
        String clientSecret = body.getClientSecret();
        List<String> scopes = body.getScopes();
        String keyType = body.getKeyType().toString();
        List<String> grantTypeList = body.getGrantTypesToBeSupported();
        String callBackUrl = body.getCallbackUrl();
        Object additionalProperties = body.getAdditionalProperties();
        String validityPeriod = body.getValidityTime();

        String[] accessAllowDomainsArray = { "ALL" };
        JSONObject jsonParamObj = new JSONObject();
        jsonParamObj.put(ApplicationConstants.OAUTH_CLIENT_USERNAME, username);
        String grantTypes = StringUtils.join(grantTypeList, ',');
        if (!StringUtils.isEmpty(grantTypes)) {
            jsonParamObj.put(APIConstants.JSON_GRANT_TYPES, grantTypes);
        }
                    /* Read clientId & clientSecret from ApplicationKeyGenerateRequestDTO object.
                       User can provide clientId only or both clientId and clientSecret
                       User cannot provide clientSecret only */
        if (!StringUtils.isEmpty(clientId)) {
            jsonParamObj.put(APIConstants.JSON_CLIENT_ID, clientId);
            if (!StringUtils.isEmpty(clientSecret)) {
                jsonParamObj.put(APIConstants.JSON_CLIENT_SECRET, clientSecret);
            }
        }

        if (additionalProperties != null) {
            if (additionalProperties instanceof String && StringUtils.isNotEmpty((String) additionalProperties)) {
                jsonParamObj.put(APIConstants.JSON_ADDITIONAL_PROPERTIES, additionalProperties);
            } else if (additionalProperties instanceof Map) {
                String jsonContent = new Gson().toJson(additionalProperties);
                jsonParamObj.put(APIConstants.JSON_ADDITIONAL_PROPERTIES, jsonContent);
            }
        }
        String jsonParams = jsonParamObj.toString();
        String tokenScopes = StringUtils.join(scopes, " ");

        return apiConsumer.requestApprovalForApplicationRegistration(username, application, keyType, callBackUrl,
                accessAllowDomainsArray, validityPeriod, tokenScopes, jsonParams, keyManagerName, organization,
                false);
    }

    /**
     * @param apiConsumer          apiConsumer
     * @param supportedGrantTypes  supportedGrantTypes
     * @param username             username
     * @param additionalAttributes additionalAttributes
     * @param application          application
     * @param keyType
     * @param callbackUrl
     * @param groupId
     * @return
     * @throws APIManagementException
     */
    private static OAuthApplicationInfo updateAuthClient(APIConsumer apiConsumer, ApplicationKeyDTO body,
            String username, Application application, String keyType) throws APIManagementException {
        String grantTypes = StringUtils.join(body.getSupportedGrantTypes(), ',');
        JsonObject jsonParams = new JsonObject();
        jsonParams.addProperty(APIConstants.JSON_GRANT_TYPES, grantTypes);
        jsonParams.addProperty(APIConstants.JSON_USERNAME, username);
        if (body.getAdditionalProperties() != null) {
            if (body.getAdditionalProperties() instanceof String && StringUtils.isNotEmpty(
                    (String) body.getAdditionalProperties())) {
                jsonParams.addProperty(APIConstants.JSON_ADDITIONAL_PROPERTIES,
                        (String) body.getAdditionalProperties());
            } else if (body.getAdditionalProperties() instanceof Map) {
                String jsonContent = new Gson().toJson(body.getAdditionalProperties());
                jsonParams.addProperty(APIConstants.JSON_ADDITIONAL_PROPERTIES, jsonContent);
            }
        }
        String keyManagerName = APIConstants.KeyManager.DEFAULT_KEY_MANAGER;

        return apiConsumer.updateAuthClient(username, application, keyType, body.getCallbackUrl(), null,
                null, null, body.getKeyMappingId(), new Gson().toJson(jsonParams), keyManagerName);
    }

}
