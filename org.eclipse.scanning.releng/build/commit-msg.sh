#!/usr/bin/env bash


last_msg="$(git log -1 --pretty=%B)"
echo "The last commit was: $last_msg"


# We check the last commit message has a ticket or starts with "Merge"
if [[ $last_msg =~ ^Merge.*|^merge.*|.*(jira\.diamond\.ac\.uk).* ]]; then
    echo "Jira ticket or merge found, build may proceed"
    exit 0
fi

# Couldn't find ticket or merge, fail build
error_msg="Aborting build. Your commit message must reference a jira ticket"
echo "$error_msg" >&2

# Get the last user
id="$(git log --format="%H" -n 1)"
user="$(git show --format="%aN <%aE>" $id)"
echo "Last commit from:"
echo " $user"

exit 1
