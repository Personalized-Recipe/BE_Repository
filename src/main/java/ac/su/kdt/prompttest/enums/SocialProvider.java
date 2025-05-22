package ac.su.kdt.prompttest.enums;

public enum SocialProvider {
    GOOGLE,
    KAKAO,
    NAVER;

    public static SocialProvider fromString(String provider) {
        try {
            return SocialProvider.valueOf(provider.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid social provider: " + provider);
        }
    }
} 