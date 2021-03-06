# Deploying

In order for Travis to automatically deploy the application to GH Releases - the repository needs to be tagged.

## Version Numbering

Standard Semantic Versioning:

Given a version number MAJOR.MINOR.PATCH, increment the:

1. MAJOR version when you make incompatible API changes,
2. MINOR version when you add functionality in a backwards compatible manner, and
3. PATCH version when you make backwards compatible bug fixes.
Additional labels for pre-release and build metadata are available as extensions to the MAJOR.MINOR.PATCH format.

## Stages 

1. In terminal, at the point you want to deploy from run `git tag v{version_number}`

2. `git push --tag`

This starts the build in Travis.

## Deleting tags

Should you want to remove a tag for any reason:

1. Delete locally `git tag -d v{version_number}`

2. Delete the tag from the remote `git push --delete origin v{version_number}`
