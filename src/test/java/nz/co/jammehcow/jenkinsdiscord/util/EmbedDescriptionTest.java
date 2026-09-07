package nz.co.jammehcow.jenkinsdiscord.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EmbedDescriptionTest {

    @Test
    void preservesFullPlasticScmChangesetId() {
        assertEquals("cs:123456", EmbedDescription.getCommitDisplayStr("cs:123456"));
    }

    @Test
    void preservesShortCommitId() {
        assertEquals("abc", EmbedDescription.getCommitDisplayStr("abc"));
    }

    @Test
    void preservesEmptyCommitId() {
        assertEquals("", EmbedDescription.getCommitDisplayStr(""));
    }

    @Test
    void preservesNullPlaceholder() {
        assertEquals(EmbedDescription.nullCommitDisplayStr, EmbedDescription.getCommitDisplayStr(null));
    }
}
