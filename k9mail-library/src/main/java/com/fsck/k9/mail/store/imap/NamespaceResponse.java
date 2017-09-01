package com.fsck.k9.mail.store.imap;


import java.util.ArrayList;
import java.util.List;

import static com.fsck.k9.mail.store.imap.ImapResponseParser.equalsIgnoreCase;


class NamespaceResponse {

    private List<Namespace> personalNamespaces;
    private List<Namespace> otherUsersNamespaces;
    private List<Namespace> sharedNamespaces;

    static class Namespace {
        private String prefix;
        private String hierarchyDelimiter;

        private Namespace(String prefix, String hierarchyDelimiter) {
            this.prefix = prefix;
            this.hierarchyDelimiter = hierarchyDelimiter;
        }

        public String getPrefix() {
            return prefix;
        }

        public String getHierarchyDelimiter() {
            return hierarchyDelimiter;
        }
    }

    private NamespaceResponse(List<Namespace> personalNamespaces,
            List<Namespace> otherUsersNamespaces, List<Namespace> sharedNamespaces) {
        this.personalNamespaces = personalNamespaces;
        this.otherUsersNamespaces = otherUsersNamespaces;
        this.sharedNamespaces = sharedNamespaces;
    }

    public static NamespaceResponse parse(List<ImapResponse> responses) {
        for (ImapResponse response : responses) {
            NamespaceResponse prefix = parse(response);
            if (prefix != null) {
                return prefix;
            }
        }

        return null;
    }

    static NamespaceResponse parse(ImapResponse response) {
        if (response.size() < 4 || !equalsIgnoreCase(response.get(0), Responses.NAMESPACE)) {
            return null;
        }

        ArrayList<Namespace> personalNamespaces = parseNamespaces(response, 1);
        ArrayList<Namespace> otherUserNamespaces = parseNamespaces(response, 2);
        ArrayList<Namespace> sharedNamespaces = parseNamespaces(response, 3);

        return new NamespaceResponse(personalNamespaces, otherUserNamespaces, sharedNamespaces);
    }

    private static ArrayList<Namespace> parseNamespaces(ImapResponse response, int key) {
        ArrayList<Namespace> namespaces = parseNamespaces(response, 1);
        if (response.isList(key)) {
            ImapList namespacesImap = response.getList(key);
            for (int i = 0; i < namespacesImap.size(); i++) {
                if (!namespacesImap.isList(i)) {
                    ImapList namespace = namespacesImap.getList(i);
                    namespaces.add(new Namespace(namespace.getString(0), namespace.getString(1)));
                }
            }
        }
        return namespaces;
    }

    public List<Namespace> getPersonalNamespaces() {
        return this.personalNamespaces;
    }

    public List<Namespace> getOtherUserNamespaces() {
        return this.otherUsersNamespaces;
    }
    public List<Namespace> getSharedNamespaces() {
        return this.sharedNamespaces;
    }
}
