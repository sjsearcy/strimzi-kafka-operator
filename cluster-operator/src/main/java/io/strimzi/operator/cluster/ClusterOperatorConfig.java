/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.operator.cluster;

import io.fabric8.kubernetes.api.model.LocalObjectReference;
import io.fabric8.kubernetes.api.model.LocalObjectReferenceBuilder;
import io.strimzi.operator.cluster.model.ImagePullPolicy;
import io.strimzi.operator.cluster.model.KafkaVersion;
import io.strimzi.operator.cluster.model.NoImageException;
import io.strimzi.operator.common.InvalidConfigurationException;
import io.strimzi.operator.common.Util;
import io.strimzi.operator.common.model.Labels;
import io.strimzi.operator.common.operator.resource.AbstractWatchableResourceOperator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableSet;

/**
 * Cluster Operator configuration
 */
public class ClusterOperatorConfig {
    private static final Logger log = LogManager.getLogger(ClusterOperatorConfig.class.getName());

    public static final String STRIMZI_NAMESPACE = "STRIMZI_NAMESPACE";
    public static final String STRIMZI_FULL_RECONCILIATION_INTERVAL_MS = "STRIMZI_FULL_RECONCILIATION_INTERVAL_MS";
    public static final String STRIMZI_OPERATION_TIMEOUT_MS = "STRIMZI_OPERATION_TIMEOUT_MS";
    public static final String STRIMZI_IMAGE_PULL_POLICY = "STRIMZI_IMAGE_PULL_POLICY";
    public static final String STRIMZI_IMAGE_PULL_SECRETS = "STRIMZI_IMAGE_PULL_SECRETS";
    public static final String STRIMZI_OPERATOR_NAMESPACE = "STRIMZI_OPERATOR_NAMESPACE";
    public static final String STRIMZI_OPERATOR_NAMESPACE_LABELS = "STRIMZI_OPERATOR_NAMESPACE_LABELS";

    // Feature Flags
    public static final String STRIMZI_RBAC_SCOPE = "STRIMZI_RBAC_SCOPE";
    public static final RbacScope DEFAULT_STRIMZI_RBAC_SCOPE = RbacScope.CLUSTER;
    public static final String STRIMZI_CREATE_CLUSTER_ROLES = "STRIMZI_CREATE_CLUSTER_ROLES";
    public static final boolean DEFAULT_CREATE_CLUSTER_ROLES = false;

    // Env vars for configuring images
    public static final String STRIMZI_KAFKA_IMAGES = "STRIMZI_KAFKA_IMAGES";
    public static final String STRIMZI_KAFKA_CONNECT_IMAGES = "STRIMZI_KAFKA_CONNECT_IMAGES";
    public static final String STRIMZI_KAFKA_CONNECT_S2I_IMAGES = "STRIMZI_KAFKA_CONNECT_S2I_IMAGES";
    public static final String STRIMZI_KAFKA_MIRROR_MAKER_IMAGES = "STRIMZI_KAFKA_MIRROR_MAKER_IMAGES";
    public static final String STRIMZI_KAFKA_MIRROR_MAKER_2_IMAGES = "STRIMZI_KAFKA_MIRROR_MAKER_2_IMAGES";
    public static final String STRIMZI_DEFAULT_TLS_SIDECAR_ENTITY_OPERATOR_IMAGE = "STRIMZI_DEFAULT_TLS_SIDECAR_ENTITY_OPERATOR_IMAGE";
    public static final String STRIMZI_DEFAULT_TLS_SIDECAR_KAFKA_IMAGE = "STRIMZI_DEFAULT_TLS_SIDECAR_KAFKA_IMAGE"; // Used only to produce warning if defined at startup
    public static final String STRIMZI_DEFAULT_TLS_SIDECAR_CRUISE_CONTROL_IMAGE = "STRIMZI_DEFAULT_TLS_SIDECAR_CRUISE_CONTROL_IMAGE";
    public static final String STRIMZI_DEFAULT_KAFKA_EXPORTER_IMAGE = "STRIMZI_DEFAULT_KAFKA_EXPORTER_IMAGE";
    public static final String STRIMZI_DEFAULT_TOPIC_OPERATOR_IMAGE = "STRIMZI_DEFAULT_TOPIC_OPERATOR_IMAGE";
    public static final String STRIMZI_DEFAULT_USER_OPERATOR_IMAGE = "STRIMZI_DEFAULT_USER_OPERATOR_IMAGE";
    public static final String STRIMZI_DEFAULT_KAFKA_INIT_IMAGE = "STRIMZI_DEFAULT_KAFKA_INIT_IMAGE";
    public static final String STRIMZI_DEFAULT_KAFKA_BRIDGE_IMAGE = "STRIMZI_DEFAULT_KAFKA_BRIDGE_IMAGE";
    public static final String STRIMZI_DEFAULT_CRUISE_CONTROL_IMAGE = "STRIMZI_DEFAULT_CRUISE_CONTROL_IMAGE";

    public static final long DEFAULT_FULL_RECONCILIATION_INTERVAL_MS = 120_000;
    public static final long DEFAULT_OPERATION_TIMEOUT_MS = 300_000;

    private final Set<String> namespaces;
    private final long reconciliationIntervalMs;
    private final long operationTimeoutMs;
    private final boolean createClusterRoles;
    private final KafkaVersion.Lookup versions;
    private final ImagePullPolicy imagePullPolicy;
    private final List<LocalObjectReference> imagePullSecrets;
    private final String operatorNamespace;
    private final Labels operatorNamespaceLabels;
    private final RbacScope rbacScope;

    /**
     * Constructor
     *
     * @param namespaces namespace in which the operator will run and create resources
     * @param reconciliationIntervalMs    specify every how many milliseconds the reconciliation runs
     * @param operationTimeoutMs    timeout for internal operations specified in milliseconds
     * @param createClusterRoles true to create the ClusterRoles
     * @param versions The configured Kafka versions
     * @param imagePullPolicy Image pull policy configured by the user
     * @param imagePullSecrets Set of secrets for pulling container images from secured repositories
     * @param operatorNamespace Name of the namespace in which the operator is running
     * @param operatorNamespaceLabels Labels of the namespace in which the operator is running (used for network policies)
     * @param rbacScope true to use Roles where possible instead of ClusterRoles
     */
    public ClusterOperatorConfig(
            Set<String> namespaces,
            long reconciliationIntervalMs,
            long operationTimeoutMs,
            boolean createClusterRoles,
            KafkaVersion.Lookup versions,
            ImagePullPolicy imagePullPolicy,
            List<LocalObjectReference> imagePullSecrets,
            String operatorNamespace,
            Labels operatorNamespaceLabels,
            RbacScope rbacScope
    ) {
        this.namespaces = unmodifiableSet(new HashSet<>(namespaces));
        this.reconciliationIntervalMs = reconciliationIntervalMs;
        this.operationTimeoutMs = operationTimeoutMs;
        this.createClusterRoles = createClusterRoles;
        this.versions = versions;
        this.imagePullPolicy = imagePullPolicy;
        this.imagePullSecrets = imagePullSecrets;
        this.operatorNamespace = operatorNamespace;
        this.operatorNamespaceLabels = operatorNamespaceLabels;
        this.rbacScope = rbacScope;
    }

    /**
     * Loads configuration parameters from a related map
     *
     * @param map   map from which loading configuration parameters
     * @return  Cluster Operator configuration instance
     */
    public static ClusterOperatorConfig fromMap(Map<String, String> map) {
        warningsForRemovedEndVars(map);
        KafkaVersion.Lookup lookup = parseKafkaVersions(map.get(STRIMZI_KAFKA_IMAGES), map.get(STRIMZI_KAFKA_CONNECT_IMAGES), map.get(STRIMZI_KAFKA_CONNECT_S2I_IMAGES), map.get(STRIMZI_KAFKA_MIRROR_MAKER_IMAGES), map.get(STRIMZI_KAFKA_MIRROR_MAKER_2_IMAGES));
        return fromMap(map, lookup);
    }

    /**
     * Logs warnings for removed / deprecated environment variables
     *
     * @param map   map from which loading configuration parameters
     */
    private static void warningsForRemovedEndVars(Map<String, String> map) {
        if (map.containsKey(STRIMZI_DEFAULT_TLS_SIDECAR_KAFKA_IMAGE))    {
            log.warn("Kafka TLS sidecar container has been removed and the environment variable {} is not used anymore. " +
                    "You can remove it from the Strimzi Cluster Operator deployment.", STRIMZI_DEFAULT_TLS_SIDECAR_KAFKA_IMAGE);
        }
    }

    /**
     * Loads configuration parameters from a related map and custom KafkaVersion.Lookup instance.
     * THis is used for testing.
     *
     * @param map   map from which loading configuration parameters
     * @param lookup KafkaVersion.Lookup instance with the supported Kafka version information
     * @return  Cluster Operator configuration instance
     */
    public static ClusterOperatorConfig fromMap(Map<String, String> map, KafkaVersion.Lookup lookup) {
        Set<String> namespaces = parseNamespaceList(map.get(STRIMZI_NAMESPACE));
        long reconciliationInterval = parseReconciliationInterval(map.get(STRIMZI_FULL_RECONCILIATION_INTERVAL_MS));
        long operationTimeout = parseOperationTimeout(map.get(STRIMZI_OPERATION_TIMEOUT_MS));
        boolean createClusterRoles = parseCreateClusterRoles(map.get(STRIMZI_CREATE_CLUSTER_ROLES));
        ImagePullPolicy imagePullPolicy = parseImagePullPolicy(map.get(STRIMZI_IMAGE_PULL_POLICY));
        List<LocalObjectReference> imagePullSecrets = parseImagePullSecrets(map.get(STRIMZI_IMAGE_PULL_SECRETS));
        String operatorNamespace = map.get(STRIMZI_OPERATOR_NAMESPACE);
        Labels operatorNamespaceLabels = parseOperatorNamespaceLabels(map.get(STRIMZI_OPERATOR_NAMESPACE_LABELS));
        RbacScope rbacScope = parseRbacScope(map.get(STRIMZI_RBAC_SCOPE));

        return new ClusterOperatorConfig(
                namespaces,
                reconciliationInterval,
                operationTimeout,
                createClusterRoles,
                lookup,
                imagePullPolicy,
                imagePullSecrets,
                operatorNamespace,
                operatorNamespaceLabels,
                rbacScope);
    }

    private static Set<String> parseNamespaceList(String namespacesList)   {
        Set<String> namespaces;
        if (namespacesList == null || namespacesList.isEmpty()) {
            namespaces = Collections.singleton(AbstractWatchableResourceOperator.ANY_NAMESPACE);
        } else {
            if (namespacesList.trim().equals(AbstractWatchableResourceOperator.ANY_NAMESPACE)) {
                namespaces = Collections.singleton(AbstractWatchableResourceOperator.ANY_NAMESPACE);
            } else if (namespacesList.matches("(\\s*[a-z0-9.-]+\\s*,)*\\s*[a-z0-9.-]+\\s*")) {
                namespaces = new HashSet<>(asList(namespacesList.trim().split("\\s*,+\\s*")));
            } else {
                throw new InvalidConfigurationException(STRIMZI_NAMESPACE
                        + " is not a valid list of namespaces nor the 'any namespace' wildcard "
                        + AbstractWatchableResourceOperator.ANY_NAMESPACE);
            }
        }

        return namespaces;
    }

    private static long parseReconciliationInterval(String reconciliationIntervalEnvVar) {
        long reconciliationInterval = DEFAULT_FULL_RECONCILIATION_INTERVAL_MS;

        if (reconciliationIntervalEnvVar != null) {
            reconciliationInterval = Long.parseLong(reconciliationIntervalEnvVar);
        }

        return reconciliationInterval;
    }

    private static long parseOperationTimeout(String operationTimeoutEnvVar) {
        long operationTimeout = DEFAULT_OPERATION_TIMEOUT_MS;

        if (operationTimeoutEnvVar != null) {
            operationTimeout = Long.parseLong(operationTimeoutEnvVar);
        }

        return operationTimeout;
    }

    private static boolean parseCreateClusterRoles(String createClusterRolesEnvVar) {
        boolean createClusterRoles = DEFAULT_CREATE_CLUSTER_ROLES;

        if (createClusterRolesEnvVar != null) {
            createClusterRoles = Boolean.parseBoolean(createClusterRolesEnvVar);
        }

        return createClusterRoles;
    }

    /**
     * enum to represent the various permission modes the cluster operator can be set to
     *
     * CLUSTER is the default and uses ClusterRoles to set permissions
     * NAMESPACE allows for the use of Roles where possible instead of ClusterRoles
     */
    public enum RbacScope {
        CLUSTER(),
        NAMESPACE();

        public boolean canUseClusterRoles() {
            return this.equals(RbacScope.CLUSTER);
        }
    }

    private static RbacScope parseRbacScope(String rbacScopeEnvVar) {
        RbacScope rbacScope = DEFAULT_STRIMZI_RBAC_SCOPE;

        if (rbacScopeEnvVar != null) {
            try {
                rbacScope = RbacScope.valueOf(rbacScopeEnvVar);
            } catch (IllegalArgumentException e) {
                throw new InvalidConfigurationException(rbacScopeEnvVar
                        + " is not a valid " + STRIMZI_RBAC_SCOPE + " value. " +
                        STRIMZI_RBAC_SCOPE + " can have one of the following values: cluster, namespace.");
            }
        }

        return rbacScope;
    }

    private static ImagePullPolicy parseImagePullPolicy(String imagePullPolicyEnvVar) {
        ImagePullPolicy imagePullPolicy = null;

        if (imagePullPolicyEnvVar != null) {
            switch (imagePullPolicyEnvVar.trim().toLowerCase(Locale.ENGLISH)) {
                case "always":
                    imagePullPolicy = ImagePullPolicy.ALWAYS;
                    break;
                case "ifnotpresent":
                    imagePullPolicy = ImagePullPolicy.IFNOTPRESENT;
                    break;
                case "never":
                    imagePullPolicy = ImagePullPolicy.NEVER;
                    break;
                default:
                    throw new InvalidConfigurationException(imagePullPolicyEnvVar
                            + " is not a valid " + STRIMZI_IMAGE_PULL_POLICY + " value. " +
                            STRIMZI_IMAGE_PULL_POLICY + " can have one of the following values: Always, IfNotPresent, Never.");
            }
        }

        return imagePullPolicy;
    }

    private static KafkaVersion.Lookup parseKafkaVersions(String kafkaImages, String connectImages, String connectS2IImages, String mirrorMakerImages, String mirrorMaker2Images) {
        KafkaVersion.Lookup lookup = new KafkaVersion.Lookup(
                Util.parseMap(kafkaImages),
                Util.parseMap(connectImages),
                Util.parseMap(connectS2IImages),
                Util.parseMap(mirrorMakerImages),
                Util.parseMap(mirrorMaker2Images));

        String image = "";
        String envVar = "";

        try {
            image = "Kafka";
            envVar = STRIMZI_KAFKA_IMAGES;
            lookup.validateKafkaImages(lookup.supportedVersions());

            image = "Kafka Connect";
            envVar = STRIMZI_KAFKA_CONNECT_IMAGES;
            lookup.validateKafkaConnectImages(lookup.supportedVersions());

            image = "Kafka Connect S2I";
            envVar = STRIMZI_KAFKA_CONNECT_S2I_IMAGES;
            lookup.validateKafkaConnectS2IImages(lookup.supportedVersions());

            image = "Kafka Mirror Maker";
            envVar = STRIMZI_KAFKA_MIRROR_MAKER_IMAGES;
            lookup.validateKafkaMirrorMakerImages(lookup.supportedVersions());

            image = "Kafka Mirror Maker 2";
            envVar = STRIMZI_KAFKA_MIRROR_MAKER_2_IMAGES;
            lookup.validateKafkaMirrorMaker2Images(lookup.supportedVersionsForFeature("kafkaMirrorMaker2"));
        } catch (NoImageException e) {
            throw new InvalidConfigurationException("Failed to parse default container image configuration for " + image + " from environment variable " + envVar, e);
        }
        return lookup;
    }

    private static List<LocalObjectReference> parseImagePullSecrets(String imagePullSecretList) {
        List<LocalObjectReference> imagePullSecrets = null;

        if (imagePullSecretList != null && !imagePullSecretList.isEmpty()) {
            if (imagePullSecretList.matches("(\\s*[a-z0-9.-]+\\s*,)*\\s*[a-z0-9.-]+\\s*")) {
                imagePullSecrets = Arrays.stream(imagePullSecretList.trim().split("\\s*,+\\s*")).map(secret -> new LocalObjectReferenceBuilder().withName(secret).build()).collect(Collectors.toList());
            } else {
                throw new InvalidConfigurationException(STRIMZI_IMAGE_PULL_SECRETS
                        + " is not a valid list of secret names");
            }
        }

        return imagePullSecrets;
    }

    private static Labels parseOperatorNamespaceLabels(String operatorNamespaceLabelsString) {
        Labels operatorNamespaceLabels = null;

        if (operatorNamespaceLabelsString != null) {
            try {
                operatorNamespaceLabels = Labels.fromString(operatorNamespaceLabelsString);
            } catch (Exception e) {
                throw new InvalidConfigurationException("Failed to parse namespace labels from " + ClusterOperatorConfig.STRIMZI_OPERATOR_NAMESPACE_LABELS, e);
            }
        }

        return operatorNamespaceLabels;
    }

    /**
     * @return  namespaces in which the operator runs and creates resources
     */
    public Set<String> getNamespaces() {
        return namespaces;
    }

    /**
     * @return  how many milliseconds the reconciliation runs
     */
    public long getReconciliationIntervalMs() {
        return reconciliationIntervalMs;
    }

    /**
     * @return  how many milliseconds should we wait for Kubernetes operations
     */
    public long getOperationTimeoutMs() {
        return operationTimeoutMs;
    }

    /**
     * @return  Indicates whether Cluster Roles should be created
     */
    public boolean isCreateClusterRoles() {
        return createClusterRoles;
    }

    public KafkaVersion.Lookup versions() {
        return versions;
    }

    /**
     * @return  The user-configure image pull policy. Null if it was not configured.
     */
    public ImagePullPolicy getImagePullPolicy() {
        return imagePullPolicy;
    }

    /**
     * @return The list of configured ImagePullSecrets. Null if no secrets were configured.
     */
    public List<LocalObjectReference> getImagePullSecrets() {
        return imagePullSecrets;
    }

    /**
     * @return Returns the name of the namespace where the operator runs or null if not configured
     */
    public String getOperatorNamespace() {
        return operatorNamespace;
    }

    /**
     * @return Returns the labels of the namespace where the operator runs or null if not configured
     */
    public Labels getOperatorNamespaceLabels() {
        return operatorNamespaceLabels;
    }

    /**
     * @return permissions mode for the operator, whether to use Roles instead of ClusterRoles wherever possible.
     */
    public RbacScope getRbacScope() {
        return rbacScope;
    }

    @Override
    public String toString() {
        return "ClusterOperatorConfig(" +
                "namespaces=" + namespaces +
                ",reconciliationIntervalMs=" + reconciliationIntervalMs +
                ",operationTimeoutMs=" + operationTimeoutMs +
                ",createClusterRoles=" + createClusterRoles +
                ",versions=" + versions +
                ",imagePullPolicy=" + imagePullPolicy +
                ",imagePullSecrets=" + imagePullSecrets +
                ",operatorNamespace=" + operatorNamespace +
                ",operatorNamespaceLabels=" + operatorNamespaceLabels +
                ",rbacScope=" + rbacScope +
                ")";
    }
}
