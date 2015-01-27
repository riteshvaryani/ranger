/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
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

 package org.apache.ranger.authorization.hive.authorizer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.permission.FsAction;
import org.apache.hadoop.hive.common.FileUtils;
import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.hive.ql.security.HiveAuthenticationProvider;
import org.apache.hadoop.hive.ql.security.authorization.plugin.HiveAccessControlException;
import org.apache.hadoop.hive.ql.security.authorization.plugin.HiveAuthzContext;
import org.apache.hadoop.hive.ql.security.authorization.plugin.HiveAuthzPluginException;
import org.apache.hadoop.hive.ql.security.authorization.plugin.HiveAuthzSessionContext;
import org.apache.hadoop.hive.ql.security.authorization.plugin.HiveMetastoreClientFactory;
import org.apache.hadoop.hive.ql.security.authorization.plugin.HiveOperationType;
import org.apache.hadoop.hive.ql.security.authorization.plugin.HivePrincipal;
import org.apache.hadoop.hive.ql.security.authorization.plugin.HivePrivilege;
import org.apache.hadoop.hive.ql.security.authorization.plugin.HivePrivilegeObject;
import org.apache.hadoop.hive.ql.security.authorization.plugin.HivePrivilegeObject.HivePrivObjectActionType;
import org.apache.hadoop.hive.ql.security.authorization.plugin.HivePrivilegeObject.HivePrivilegeObjectType;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.ranger.admin.client.RangerAdminRESTClient;
import org.apache.ranger.admin.client.datatype.GrantRevokeData;
import org.apache.ranger.authorization.hadoop.config.RangerConfiguration;
import org.apache.ranger.authorization.hadoop.constants.RangerHadoopConstants;
import org.apache.ranger.authorization.utils.StringUtil;
import org.apache.ranger.plugin.policyengine.RangerAccessRequest;
import org.apache.ranger.plugin.policyengine.RangerAccessResult;
import org.apache.ranger.plugin.policyengine.RangerAccessResult.Result;
import org.apache.ranger.plugin.service.RangerBasePlugin;

import com.google.common.collect.Sets;

public class RangerHiveAuthorizer extends RangerHiveAuthorizerBase {
	private static final Log LOG = LogFactory.getLog(RangerHiveAuthorizer.class) ; 

	private static final char COLUMN_SEP = ',';

	private static final boolean UpdateXaPoliciesOnGrantRevoke = RangerConfiguration.getInstance().getBoolean(RangerHadoopConstants.HIVE_UPDATE_RANGER_POLICIES_ON_GRANT_REVOKE_PROP, RangerHadoopConstants.HIVE_UPDATE_RANGER_POLICIES_ON_GRANT_REVOKE_DEFAULT_VALUE);

	private static RangerHivePlugin hivePlugin = null ;


	public RangerHiveAuthorizer(HiveMetastoreClientFactory metastoreClientFactory,
								  HiveConf                   hiveConf,
								  HiveAuthenticationProvider hiveAuthenticator,
								  HiveAuthzSessionContext    sessionContext) {
		super(metastoreClientFactory, hiveConf, hiveAuthenticator, sessionContext);

		LOG.debug("RangerHiveAuthorizer.RangerHiveAuthorizer()");

		if(hivePlugin == null) {
			synchronized(RangerHiveAuthorizer.class) {
				if(hivePlugin == null) {
					RangerHivePlugin temp = new RangerHivePlugin();
					temp.init();
					
					if(!RangerConfiguration.getInstance().isAuditInitDone()) {
						if(sessionContext != null) {
							String appType = "unknown";

							switch(sessionContext.getClientType()) {
								case HIVECLI:
									appType = "hiveCLI";
								break;

								case HIVESERVER2:
									appType = "hiveServer2";
								break;
							}

							RangerConfiguration.getInstance().initAudit(appType);
						}
					}

					hivePlugin = temp;
				}
			}
		}
	}


	/**
	 * Grant privileges for principals on the object
	 * @param hivePrincipals
	 * @param hivePrivileges
	 * @param hivePrivObject
	 * @param grantorPrincipal
	 * @param grantOption
	 * @throws HiveAuthzPluginException
	 * @throws HiveAccessControlException
	 */
	@Override
	public void grantPrivileges(List<HivePrincipal> hivePrincipals,
								List<HivePrivilege> hivePrivileges,
								HivePrivilegeObject hivePrivObject,
								HivePrincipal       grantorPrincipal,
								boolean             grantOption)
										throws HiveAuthzPluginException, HiveAccessControlException {
		if(! UpdateXaPoliciesOnGrantRevoke) {
			throw new HiveAuthzPluginException("GRANT/REVOKE not supported in Ranger HiveAuthorizer. Please use Ranger Security Admin to setup access control.");
		}

		/* TODO:
		 * 
		boolean                isSuccess     = false;
		RangerHiveObjectAccessInfo objAccessInfo = getHiveAccessRequests(HiveOperationType.GRANT_PRIVILEGE, hivePrivObject, new RangerHiveAccessContext(null, getHiveAuthzSessionContext()), true);

		try {
			GrantRevokeData grData = createGrantRevokeData(objAccessInfo, hivePrincipals, hivePrivileges, getGrantorUsername(grantorPrincipal), grantOption);

			if(LOG.isDebugEnabled()) {
				LOG.debug("grantPrivileges(): " + grData.toJson());
			}

			RangerAdminRESTClient xaAdmin = new RangerAdminRESTClient();

		    xaAdmin.grantPrivilege(grData);

		    isSuccess = true;
		} catch(Exception excp) {
			throw new HiveAccessControlException(excp);
		} finally {
			if(mHiveAccessVerifier.isAudited(objAccessInfo)) {
				UserGroupInformation ugi = this.getCurrentUserGroupInfo();

				// Note: failed return from REST call will be logged as 'DENIED'
				logAuditEvent(ugi, objAccessInfo, isSuccess);
			}
		}
		*/
	}

	/**
	 * Revoke privileges for principals on the object
	 * @param hivePrincipals
	 * @param hivePrivileges
	 * @param hivePrivObject
	 * @param grantorPrincipal
	 * @param grantOption
	 * @throws HiveAuthzPluginException
	 * @throws HiveAccessControlException
	 */
	@Override
	public void revokePrivileges(List<HivePrincipal> hivePrincipals,
								 List<HivePrivilege> hivePrivileges,
								 HivePrivilegeObject hivePrivObject,
								 HivePrincipal       grantorPrincipal,
								 boolean             grantOption)
										 throws HiveAuthzPluginException, HiveAccessControlException {
		if(! UpdateXaPoliciesOnGrantRevoke) {
			throw new HiveAuthzPluginException("GRANT/REVOKE not supported in Ranger HiveAuthorizer. Please use Ranger Security Admin to setup access control.");
		}

		/* TODO:
		 * 
		boolean                isSuccess     = false;
		RangerHiveObjectAccessInfo objAccessInfo = getHiveAccessRequests(HiveOperationType.REVOKE_PRIVILEGE, hivePrivObject, new RangerHiveAccessContext(null, getHiveAuthzSessionContext()), true);

		try {
			GrantRevokeData grData = createGrantRevokeData(objAccessInfo, hivePrincipals, hivePrivileges, getGrantorUsername(grantorPrincipal), grantOption);

			if(LOG.isDebugEnabled()) {
				LOG.debug("revokePrivileges(): " + grData.toJson());
			}

			RangerAdminRESTClient xaAdmin = new RangerAdminRESTClient();

		    xaAdmin.revokePrivilege(grData);

		    isSuccess = true;
		} catch(Exception excp) {
			throw new HiveAccessControlException(excp);
		} finally {
			if(mHiveAccessVerifier.isAudited(objAccessInfo)) {
				UserGroupInformation ugi = this.getCurrentUserGroupInfo();

				// Note: failed return from REST call will be logged as 'DENIED'
				logAuditEvent(ugi, objAccessInfo, isSuccess);
			}
		}
		*/
	}

	/**
	 * Check if user has privileges to do this action on these objects
	 * @param hiveOpType
	 * @param inputsHObjs
	 * @param outputHObjs
	 * @param context
	 * @throws HiveAuthzPluginException
	 * @throws HiveAccessControlException
	 */
	@Override
	public void checkPrivileges(HiveOperationType         hiveOpType,
								List<HivePrivilegeObject> inputHObjs,
							    List<HivePrivilegeObject> outputHObjs,
							    HiveAuthzContext          context)
		      throws HiveAuthzPluginException, HiveAccessControlException {
		UserGroupInformation ugi = getCurrentUserGroupInfo();

		if(ugi == null) {
			throw new HiveAccessControlException("Permission denied: user information not available");
		}

		RangerHiveAuditHandler auditHandler = new RangerHiveAuditHandler();

		try {
			HiveAuthzSessionContext sessionContext = getHiveAuthzSessionContext();
			String                  user           = ugi.getShortUserName();
			Set<String>             groups         = Sets.newHashSet(ugi.getGroupNames());

			if(LOG.isDebugEnabled()) {
				LOG.debug(toString(hiveOpType, inputHObjs, outputHObjs, context, sessionContext));
			}

			if(hiveOpType == HiveOperationType.DFS) {
				handleDfsCommand(hiveOpType, inputHObjs, outputHObjs, context, sessionContext, user, groups, auditHandler);

				return;
			}

			List<RangerHiveAccessRequest> requests = new ArrayList<RangerHiveAccessRequest>();

			if(inputHObjs != null) {
				for(HivePrivilegeObject hiveObj : inputHObjs) {
					RangerHiveResource resource = getHiveResource(hiveOpType, hiveObj);

					if(resource.getObjectType() == HiveObjectType.URI) {
						String   path       = hiveObj.getObjectName();
						FsAction permission = FsAction.READ;

		                if(!isURIAccessAllowed(user, groups, permission, path, getHiveConf())) {
		    				throw new HiveAccessControlException(String.format("Permission denied: user [%s] does not have [%s] privilege on [%s]", user, permission.name(), path));
		                }

						continue;
					}

					HiveAccessType accessType = getAccessType(hiveObj, hiveOpType, true);

					// ADMIN: access check is performed at the Ranger policy server, as a part of updating the permissions
					if(accessType == HiveAccessType.ADMIN || accessType == HiveAccessType.NONE) {
						continue;
					}

					if(!existsByResourceAndAccessType(requests, resource, accessType)) {
						RangerHiveAccessRequest request = new RangerHiveAccessRequest(resource, user, groups, hiveOpType, accessType, context, sessionContext);

						requests.add(request);
					}
				}
			}

			if(outputHObjs != null) {
				for(HivePrivilegeObject hiveObj : outputHObjs) {
					RangerHiveResource resource = getHiveResource(hiveOpType, hiveObj);

					if(resource.getObjectType() == HiveObjectType.URI) {
						String   path       = hiveObj.getObjectName();
						FsAction permission = FsAction.WRITE;

		                if(!isURIAccessAllowed(user, groups, permission, path, getHiveConf())) {
		    				throw new HiveAccessControlException(String.format("Permission denied: user [%s] does not have [%s] privilege on [%s]", user, permission.name(), path));
		                }

						continue;
					}

					HiveAccessType accessType = getAccessType(hiveObj, hiveOpType, false);

					// ADMIN: access check is performed at the Ranger policy server, as a part of updating the permissions
					if(accessType == HiveAccessType.ADMIN || accessType == HiveAccessType.NONE) {
						continue;
					}

					if(!existsByResourceAndAccessType(requests, resource, accessType)) {
						RangerHiveAccessRequest request = new RangerHiveAccessRequest(resource, user, groups, hiveOpType, accessType, context, sessionContext);

						requests.add(request);
					}
				}
			}

			for(RangerHiveAccessRequest request : requests) {
	            RangerHiveResource resource = (RangerHiveResource)request.getResource();
	            RangerAccessResult result   = null;

	            if(resource.getObjectType() == HiveObjectType.COLUMN && StringUtils.contains(resource.getColumn(), COLUMN_SEP)) {
	            	List<RangerAccessRequest> colRequests = new ArrayList<RangerAccessRequest>();

	            	String[] columns = StringUtils.split(resource.getColumn(), COLUMN_SEP);

	            	for(String column : columns) {
	            		column = column == null ? null : column.trim();

	            		if(StringUtils.isEmpty(column.trim())) {
	            			continue;
	            		}

	            		RangerHiveResource colResource = new RangerHiveResource(HiveObjectType.COLUMN, resource.getDatabase(), resource.getTableOrUdf(), column);

	            		RangerHiveAccessRequest colRequest = request.copy();
	            		colRequest.setResource(colResource);

	            		colRequests.add(colRequest);
	            	}

	            	Collection<RangerAccessResult> colResults = hivePlugin.isAccessAllowed(colRequests, auditHandler);

	            	if(colResults != null) {
		            	for(RangerAccessResult colResult : colResults) {
		            		result = colResult;

		            		if(result.getResult() != Result.ALLOWED) {
		            			break;
		            		}
		            	}
	            	}
	            } else {
		            result = hivePlugin.isAccessAllowed(request, auditHandler);
	            }

				if(result != null && result.getResult() != Result.ALLOWED) {
					String path = auditHandler.getResourceValueAsString(request.getResource(), result.getServiceDef());
	
					throw new HiveAccessControlException(String.format("Permission denied: user [%s] does not have [%s] privilege on [%s]",
														 user, request.getAccessType().name(), path));
				}
			}
		} finally {
			auditHandler.flushAudit();
		}
	}

	private RangerHiveResource getHiveResource(HiveOperationType   hiveOpType,
											   HivePrivilegeObject hiveObj) {
		RangerHiveResource ret = null;

		HiveObjectType objectType = getObjectType(hiveObj, hiveOpType);

		switch(objectType) {
			case DATABASE:
				ret = new RangerHiveResource(objectType, hiveObj.getDbname());
			break;
	
			case TABLE:
			case VIEW:
			case PARTITION:
			case INDEX:
			case FUNCTION:
				ret = new RangerHiveResource(objectType, hiveObj.getDbname(), hiveObj.getObjectName());
			break;
	
			case COLUMN:
				ret = new RangerHiveResource(objectType, hiveObj.getDbname(), hiveObj.getObjectName(), StringUtils.join(hiveObj.getColumns(), COLUMN_SEP));
			break;

            case URI:
				ret = new RangerHiveResource(objectType, hiveObj.getObjectName());
            break;

			case NONE:
			break;
		}

		return ret;
	}

	private HiveObjectType getObjectType(HivePrivilegeObject hiveObj, HiveOperationType hiveOpType) {
		HiveObjectType objType = HiveObjectType.NONE;

		switch(hiveObj.getType()) {
			case DATABASE:
				objType = HiveObjectType.DATABASE;
			break;

			case PARTITION:
				objType = HiveObjectType.PARTITION;
			break;

			case TABLE_OR_VIEW:
				String hiveOpTypeName = hiveOpType.name().toLowerCase();
				if(hiveOpTypeName.contains("index")) {
					objType = HiveObjectType.INDEX;
				} else if(! StringUtil.isEmpty(hiveObj.getColumns())) {
					objType = HiveObjectType.COLUMN;
				} else if(hiveOpTypeName.contains("view")) {
					objType = HiveObjectType.VIEW;
				} else {
					objType = HiveObjectType.TABLE;
				}
			break;

			case FUNCTION:
				objType = HiveObjectType.FUNCTION;
			break;

			case DFS_URI:
			case LOCAL_URI:
                objType = HiveObjectType.URI;
            break;

			case COMMAND_PARAMS:
			case GLOBAL:
			break;

			case COLUMN:
				// Thejas: this value is unused in Hive; the case should not be hit.
			break;
		}

		return objType;
	}
	
	private HiveAccessType getAccessType(HivePrivilegeObject hiveObj, HiveOperationType hiveOpType, boolean isInput) {
		HiveAccessType           accessType       = HiveAccessType.NONE;
		HivePrivObjectActionType objectActionType = hiveObj.getActionType();
		
		switch(objectActionType) {
			case INSERT:
			case INSERT_OVERWRITE:
			case UPDATE:
			case DELETE:
				accessType = HiveAccessType.UPDATE;
			break;
			case OTHER:
			switch(hiveOpType) {
				case CREATEDATABASE:
					if(hiveObj.getType() == HivePrivilegeObjectType.DATABASE) {
						accessType = HiveAccessType.CREATE;
					}
				break;

				case CREATEFUNCTION:
					if(hiveObj.getType() == HivePrivilegeObjectType.FUNCTION) {
						accessType = HiveAccessType.CREATE;
					}
				break;

				case CREATETABLE:
				case CREATEVIEW:
				case CREATETABLE_AS_SELECT:
					if(hiveObj.getType() == HivePrivilegeObjectType.TABLE_OR_VIEW) {
						accessType = isInput ? HiveAccessType.SELECT : HiveAccessType.CREATE;
					}
				break;

				case ALTERDATABASE:
				case ALTERDATABASE_OWNER:
				case ALTERINDEX_PROPS:
				case ALTERINDEX_REBUILD:
				case ALTERPARTITION_BUCKETNUM:
				case ALTERPARTITION_FILEFORMAT:
				case ALTERPARTITION_LOCATION:
				case ALTERPARTITION_MERGEFILES:
				case ALTERPARTITION_PROTECTMODE:
				case ALTERPARTITION_SERDEPROPERTIES:
				case ALTERPARTITION_SERIALIZER:
				case ALTERTABLE_ADDCOLS:
				case ALTERTABLE_ADDPARTS:
				case ALTERTABLE_ARCHIVE:
				case ALTERTABLE_BUCKETNUM:
				case ALTERTABLE_CLUSTER_SORT:
				case ALTERTABLE_COMPACT:
				case ALTERTABLE_DROPPARTS:
				case ALTERTABLE_FILEFORMAT:
				case ALTERTABLE_LOCATION:
				case ALTERTABLE_MERGEFILES:
				case ALTERTABLE_PARTCOLTYPE:
				case ALTERTABLE_PROPERTIES:
				case ALTERTABLE_PROTECTMODE:
				case ALTERTABLE_RENAME:
				case ALTERTABLE_RENAMECOL:
				case ALTERTABLE_RENAMEPART:
				case ALTERTABLE_REPLACECOLS:
				case ALTERTABLE_SERDEPROPERTIES:
				case ALTERTABLE_SERIALIZER:
				case ALTERTABLE_SKEWED:
				case ALTERTABLE_TOUCH:
				case ALTERTABLE_UNARCHIVE:
				case ALTERTABLE_UPDATEPARTSTATS:
				case ALTERTABLE_UPDATETABLESTATS:
				case ALTERTBLPART_SKEWED_LOCATION:
				case ALTERVIEW_AS:
				case ALTERVIEW_PROPERTIES:
				case ALTERVIEW_RENAME:
				case DROPVIEW_PROPERTIES:
					accessType = HiveAccessType.ALTER;
				break;

				case DROPFUNCTION:
				case DROPINDEX:
				case DROPTABLE:
				case DROPVIEW:
				case DROPDATABASE:
					accessType = HiveAccessType.DROP;
				break;

				case CREATEINDEX:
					accessType = HiveAccessType.INDEX;
				break;

				case IMPORT:
				case EXPORT:
				case LOAD:
					accessType = isInput ? HiveAccessType.SELECT : HiveAccessType.UPDATE;
				break;

				case LOCKDB:
				case LOCKTABLE:
				case UNLOCKDB:
				case UNLOCKTABLE:
					accessType = HiveAccessType.LOCK;
				break;

				case QUERY:
				case SHOW_TABLESTATUS:
				case SHOW_CREATETABLE:
				case SHOWCOLUMNS:
				case SHOWINDEXES:
				case SHOWPARTITIONS:
				case SHOW_TBLPROPERTIES:
				case DESCTABLE:
				case ANALYZE_TABLE:
					accessType = HiveAccessType.SELECT;
				break;

				case SWITCHDATABASE:
				case DESCDATABASE:
					accessType = HiveAccessType.USE;
				break;

				case TRUNCATETABLE:
					accessType = HiveAccessType.UPDATE;
				break;

				case GRANT_PRIVILEGE:
				case REVOKE_PRIVILEGE:
					accessType = HiveAccessType.ADMIN;
				break;

				case ADD:
				case DELETE:
				case COMPILE:
				case CREATEMACRO:
				case CREATEROLE:
				case DESCFUNCTION:
				case DFS:
				case DROPMACRO:
				case DROPROLE:
				case EXPLAIN:
				case GRANT_ROLE:
				case MSCK:
				case REVOKE_ROLE:
				case RESET:
				case SET:
				case SHOWCONF:
				case SHOWDATABASES:
				case SHOWFUNCTIONS:
				case SHOWLOCKS:
				case SHOWTABLES:
				case SHOW_COMPACTIONS:
				case SHOW_GRANT:
				case SHOW_ROLES:
				case SHOW_ROLE_GRANT:
				case SHOW_ROLE_PRINCIPALS:
				case SHOW_TRANSACTIONS:
				break;
			}
			break;
		}
		
		return accessType;
	}

    private boolean isURIAccessAllowed(String userName, Set<String> groups, FsAction action, String uri, HiveConf conf) {
        boolean ret = false;

        if(action == FsAction.NONE) {
            ret = true;
        } else {
            try {
                Path       filePath   = new Path(uri);
                FileSystem fs         = FileSystem.get(filePath.toUri(), conf);
                Path       path       = FileUtils.getPathOrParentThatExists(fs, filePath);
                FileStatus fileStatus = fs.getFileStatus(path);

                if (FileUtils.isOwnerOfFileHierarchy(fs, fileStatus, userName)) {
                    ret = true;
                } else {
                    ret = FileUtils.isActionPermittedForFileHierarchy(fs, fileStatus, userName, action);
                }
            } catch(Exception excp) {
                LOG.error("Error getting permissions for " + uri, excp);
            }
        }

        return ret;
    }

	private void handleDfsCommand(HiveOperationType         hiveOpType,
								  List<HivePrivilegeObject> inputHObjs,
							      List<HivePrivilegeObject> outputHObjs,
							      HiveAuthzContext          context,
							      HiveAuthzSessionContext   sessionContext,
								  String                    user,
								  Set<String>               groups,
								  RangerHiveAuditHandler    auditHandler)
	      throws HiveAuthzPluginException, HiveAccessControlException {

		String dfsCommandParams = null;

		if(inputHObjs != null) {
			for(HivePrivilegeObject hiveObj : inputHObjs) {
				if(hiveObj.getType() == HivePrivilegeObjectType.COMMAND_PARAMS) {
					dfsCommandParams = StringUtil.toString(hiveObj.getCommandParams());

					if(! StringUtil.isEmpty(dfsCommandParams)) {
						break;
					}
				}
			}
		}

		int    serviceType = -1;
		String serviceName = null;

		if(hivePlugin != null) {
			if(hivePlugin.getPolicyEngine() != null &&
			   hivePlugin.getPolicyEngine().getServiceDef() != null &&
			   hivePlugin.getPolicyEngine().getServiceDef().getId() != null ) {
				serviceType = hivePlugin.getPolicyEngine().getServiceDef().getId().intValue();
			}

			serviceName = hivePlugin.getServiceName();
		}

		auditHandler.logAuditEventForDfs(user, dfsCommandParams, false, serviceType, serviceName);

		throw new HiveAccessControlException(String.format("Permission denied: user [%s] does not have privilege for [%s] command",
											 user, hiveOpType.name()));
	}

	private boolean existsByResourceAndAccessType(Collection<RangerHiveAccessRequest> requests, RangerHiveResource resource, HiveAccessType accessType) {
		boolean ret = false;

		if(requests != null && resource != null) {
			for(RangerHiveAccessRequest request : requests) {
				if(request.getAccessType() == accessType && request.getResource().equals(resource)) {
					ret = true;

					break;
				}
			}
		}

		return ret;
	}

	/*
	private String getGrantorUsername(HivePrincipal grantorPrincipal) {
		String grantor = grantorPrincipal != null ? grantorPrincipal.getName() : null;

		if(StringUtil.isEmpty(grantor)) {
			UserGroupInformation ugi = this.getCurrentUserGroupInfo();

			grantor = ugi != null ? ugi.getShortUserName() : null;
		}

		return grantor;
	}

	private GrantRevokeData createGrantRevokeData(RangerHiveObjectAccessInfo objAccessInfo,
												  List<HivePrincipal>    hivePrincipals,
												  List<HivePrivilege>    hivePrivileges,
												  String                 grantor,
												  boolean                grantOption)
														  throws HiveAccessControlException {
		if(objAccessInfo == null ||
		  ! (   objAccessInfo.getObjectType() == HiveObjectType.DATABASE
		     || objAccessInfo.getObjectType() == HiveObjectType.TABLE
		     || objAccessInfo.getObjectType() == HiveObjectType.VIEW
		     || objAccessInfo.getObjectType() == HiveObjectType.COLUMN
		   )
		  ) {
			throw new HiveAccessControlException("grantPrivileges(): unexpected object type '" + objAccessInfo.getObjectType().name());
		}

		String database = objAccessInfo.getDatabase();
		String table    = objAccessInfo.getObjectType() == HiveObjectType.VIEW ? objAccessInfo.getView() : objAccessInfo.getTable();
		String columns  = StringUtil.toString(objAccessInfo.getColumns());

		GrantRevokeData.PermMap permMap = new GrantRevokeData.PermMap ();

		for(HivePrivilege privilege : hivePrivileges) {
			String privName = privilege.getName();

			if(StringUtil.equalsIgnoreCase(privName, HiveAccessType.ALL.name())) {
				permMap.addPerm(HiveAccessType.ALL.name());
			} else if(StringUtil.equalsIgnoreCase(privName, HiveAccessType.ALTER.name())) {
				permMap.addPerm(HiveAccessType.ALTER.name());
			} else if(StringUtil.equalsIgnoreCase(privName, HiveAccessType.CREATE.name())) {
				permMap.addPerm(HiveAccessType.CREATE.name());
			} else if(StringUtil.equalsIgnoreCase(privName, HiveAccessType.DROP.name())) {
				permMap.addPerm(HiveAccessType.DROP.name());
			} else if(StringUtil.equalsIgnoreCase(privName, HiveAccessType.INDEX.name())) {
				permMap.addPerm(HiveAccessType.INDEX.name());
			} else if(StringUtil.equalsIgnoreCase(privName, HiveAccessType.LOCK.name())) {
				permMap.addPerm(HiveAccessType.LOCK.name());
			} else if(StringUtil.equalsIgnoreCase(privName, HiveAccessType.SELECT.name())) {
				permMap.addPerm(HiveAccessType.SELECT.name());
			} else if(StringUtil.equalsIgnoreCase(privName, HiveAccessType.UPDATE.name())) {
				permMap.addPerm(HiveAccessType.UPDATE.name());
			}
		}

		if(grantOption) {
			permMap.addPerm(HiveAccessType.ADMIN.name());
		}

		for(HivePrincipal principal : hivePrincipals) {
			switch(principal.getType()) {
				case USER:
					permMap.addUser(principal.getName());
				break;

				case GROUP:
				case ROLE:
					permMap.addGroup(principal.getName());
				break;

				default:
				break;
			}
		}

		GrantRevokeData grData = new GrantRevokeData();

		grData.setHiveData(grantor, repositoryName, database, table, columns, permMap);

		return grData;
	}
	*/
	
	private String toString(HiveOperationType         hiveOpType,
							List<HivePrivilegeObject> inputHObjs,
							List<HivePrivilegeObject> outputHObjs,
							HiveAuthzContext          context,
							HiveAuthzSessionContext   sessionContext) {
		StringBuilder sb = new StringBuilder();
		
		sb.append("'checkPrivileges':{");
		sb.append("'hiveOpType':").append(hiveOpType);

		sb.append(", 'inputHObjs':[");
		toString(inputHObjs, sb);
		sb.append("]");

		sb.append(", 'outputHObjs':[");
		toString(outputHObjs, sb);
		sb.append("]");

		sb.append(", 'context':{");
		sb.append("'clientType':").append(sessionContext == null ? null : sessionContext.getClientType());
		sb.append(", 'commandString':").append(context == null ? null : context.getCommandString());
		sb.append(", 'ipAddress':").append(context == null ? null : context.getIpAddress());
		sb.append(", 'sessionString':").append(sessionContext == null ? null : sessionContext.getSessionString());
		sb.append("}");

		sb.append(", 'user':").append(this.getCurrentUserGroupInfo().getUserName());
		sb.append(", 'groups':[").append(StringUtil.toString(this.getCurrentUserGroupInfo().getGroupNames())).append("]");

		sb.append("}");

		return sb.toString();
	}

	private StringBuilder toString(List<HivePrivilegeObject> privObjs, StringBuilder sb) {
		if(privObjs != null && privObjs.size() > 0) {
			toString(privObjs.get(0), sb);
			for(int i = 1; i < privObjs.size(); i++) {
				sb.append(",");
				toString(privObjs.get(i), sb);
			}
		}
		
		return sb;
	}

	private StringBuilder toString(HivePrivilegeObject privObj, StringBuilder sb) {
		sb.append("'HivePrivilegeObject':{");
		sb.append("'type':").append(privObj.getType().toString());
		sb.append(", 'dbName':").append(privObj.getDbname());
		sb.append(", 'objectType':").append(privObj.getType());
		sb.append(", 'objectName':").append(privObj.getObjectName());
		sb.append(", 'columns':[").append(StringUtil.toString(privObj.getColumns())).append("]");
		sb.append(", 'partKeys':[").append(StringUtil.toString(privObj.getPartKeys())).append("]");
		sb.append(", 'commandParams':[").append(StringUtil.toString(privObj.getCommandParams())).append("]");
		sb.append(", 'actionType':").append(privObj.getActionType().toString());
		sb.append("}");

		return sb;
	}
}

enum HiveObjectType { NONE, DATABASE, TABLE, VIEW, PARTITION, INDEX, COLUMN, FUNCTION, URI };
enum HiveAccessType { NONE, CREATE, ALTER, DROP, INDEX, LOCK, SELECT, UPDATE, USE, ALL, ADMIN };

class RangerHivePlugin extends RangerBasePlugin {
	public RangerHivePlugin() {
		super("hive");
	}
}


