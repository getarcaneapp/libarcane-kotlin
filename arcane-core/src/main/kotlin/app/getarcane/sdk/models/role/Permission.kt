package app.getarcane.sdk.models.role

/**
 * Well-known permission strings recognized by the Arcane v2 RBAC system — a namespace of string
 * constants, not an enum. Custom permission strings are also accepted; the SDK treats permissions
 * as free-form strings.
 */
public object Permission {
    /** Sudo wildcard. A user holding `"*"` in any in-scope bucket passes every permission check. */
    public const val SUDO: String = "*"

    public object Containers {
        public const val LIST: String = "containers:list"
        public const val READ: String = "containers:read"
        public const val LOGS: String = "containers:logs"
        public const val CREATE: String = "containers:create"
        public const val START: String = "containers:start"
        public const val STOP: String = "containers:stop"
        public const val RESTART: String = "containers:restart"
        public const val REDEPLOY: String = "containers:redeploy"
        public const val DELETE: String = "containers:delete"
        public const val EXEC: String = "containers:exec"
        public const val AUTO_UPDATE: String = "containers:autoupdate"
    }

    public object Projects {
        public const val LIST: String = "projects:list"
        public const val READ: String = "projects:read"
        public const val LOGS: String = "projects:logs"
        public const val CREATE: String = "projects:create"
        public const val UPDATE: String = "projects:update"
        public const val DEPLOY: String = "projects:deploy"
        public const val DOWN: String = "projects:down"
        public const val RESTART: String = "projects:restart"
        public const val DELETE: String = "projects:delete"
        public const val ARCHIVE: String = "projects:archive"
    }

    public object Images {
        public const val LIST: String = "images:list"
        public const val READ: String = "images:read"
        public const val PULL: String = "images:pull"
        public const val PUSH: String = "images:push"
        public const val BUILD: String = "images:build"
        public const val PRUNE: String = "images:prune"
        public const val DELETE: String = "images:delete"
        public const val UPLOAD: String = "images:upload"
    }

    public object Volumes {
        public const val LIST: String = "volumes:list"
        public const val READ: String = "volumes:read"
        public const val CREATE: String = "volumes:create"
        public const val DELETE: String = "volumes:delete"
        public const val PRUNE: String = "volumes:prune"
        public const val BROWSE: String = "volumes:browse"
        public const val UPLOAD: String = "volumes:upload"
        public const val BACKUP: String = "volumes:backup"
    }

    public object Networks {
        public const val LIST: String = "networks:list"
        public const val READ: String = "networks:read"
        public const val CREATE: String = "networks:create"
        public const val DELETE: String = "networks:delete"
        public const val PRUNE: String = "networks:prune"
    }

    public object Swarm {
        public const val READ: String = "swarm:read"
        public const val INIT: String = "swarm:init"
        public const val JOIN: String = "swarm:join"
        public const val LEAVE: String = "swarm:leave"
        public const val SPEC: String = "swarm:spec"
        public const val NODES: String = "swarm:nodes"
        public const val SERVICES: String = "swarm:services"
        public const val SERVICES_LOGS: String = "swarm:services:logs"
        public const val STACKS: String = "swarm:stacks"
        public const val CONFIGS: String = "swarm:configs"
        public const val SECRETS: String = "swarm:secrets"
        public const val UNLOCK: String = "swarm:unlock"
    }

    public object Users {
        public const val LIST: String = "users:list"
        public const val READ: String = "users:read"
        public const val CREATE: String = "users:create"
        public const val UPDATE: String = "users:update"
        public const val DELETE: String = "users:delete"
    }

    public object Roles {
        public const val LIST: String = "roles:list"
        public const val READ: String = "roles:read"
    }

    public object ApiKeys {
        public const val LIST: String = "apikeys:list"
        public const val READ: String = "apikeys:read"
        public const val CREATE: String = "apikeys:create"
        public const val UPDATE: String = "apikeys:update"
        public const val DELETE: String = "apikeys:delete"
    }

    public object Settings {
        public const val READ: String = "settings:read"
        public const val WRITE: String = "settings:write"
    }

    public object Environments {
        public const val LIST: String = "environments:list"
        public const val READ: String = "environments:read"
        public const val CREATE: String = "environments:create"
        public const val UPDATE: String = "environments:update"
        public const val DELETE: String = "environments:delete"
        public const val PAIR: String = "environments:pair"
        public const val SYNC: String = "environments:sync"
    }

    public object Registries {
        public const val LIST: String = "registries:list"
        public const val READ: String = "registries:read"
        public const val CREATE: String = "registries:create"
        public const val UPDATE: String = "registries:update"
        public const val DELETE: String = "registries:delete"
        public const val TEST: String = "registries:test"
    }

    public object Templates {
        public const val LIST: String = "templates:list"
        public const val READ: String = "templates:read"
        public const val CREATE: String = "templates:create"
        public const val UPDATE: String = "templates:update"
        public const val DELETE: String = "templates:delete"
    }

    public object GitRepositories {
        public const val LIST: String = "git-repositories:list"
        public const val READ: String = "git-repositories:read"
        public const val CREATE: String = "git-repositories:create"
        public const val UPDATE: String = "git-repositories:update"
        public const val DELETE: String = "git-repositories:delete"
        public const val TEST: String = "git-repositories:test"
        public const val SYNC: String = "git-repositories:sync"
    }

    public object GitOps {
        public const val LIST: String = "gitops:list"
        public const val READ: String = "gitops:read"
        public const val CREATE: String = "gitops:create"
        public const val UPDATE: String = "gitops:update"
        public const val DELETE: String = "gitops:delete"
        public const val SYNC: String = "gitops:sync"
    }

    public object Webhooks {
        public const val LIST: String = "webhooks:list"
        public const val CREATE: String = "webhooks:create"
        public const val UPDATE: String = "webhooks:update"
        public const val DELETE: String = "webhooks:delete"
    }

    public object System {
        public const val READ: String = "system:read"
        public const val PRUNE: String = "system:prune"
        public const val UPGRADE: String = "system:upgrade"
    }

    public object Vulnerabilities {
        public const val READ: String = "vulnerabilities:read"
        public const val SCAN: String = "vulnerabilities:scan"
        public const val MANAGE: String = "vulnerabilities:manage"
    }

    public object ImageUpdates {
        public const val READ: String = "image-updates:read"
        public const val CHECK: String = "image-updates:check"
    }

    public object Events {
        public const val READ: String = "events:read"
    }

    public object Dashboard {
        public const val READ: String = "dashboard:read"
    }

    public object Jobs {
        public const val MANAGE: String = "jobs:manage"
    }

    public object Notifications {
        public const val MANAGE: String = "notifications:manage"
    }

    public object Customize {
        public const val MANAGE: String = "customize:manage"
    }

    public object BuildWorkspaces {
        public const val MANAGE: String = "build-workspaces:manage"
    }
}
