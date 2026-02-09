package org.phonepe;

import org.phonepe.expiry.BackgroundExpiryRunner;
import org.phonepe.expiry.DefaultExpiryHandler;
import org.phonepe.expiry.ExpiryHandler;
import org.phonepe.model.CampaignResult;
import org.phonepe.model.CampaignType;
import org.phonepe.model.ScoreEntry;
import org.phonepe.repository.IGameRepository;
import org.phonepe.repository.impl.InMemoryGameRepository;
import org.phonepe.service.ILeaderboardService;
import org.phonepe.service.LeaderboardService;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class LeaderboardApplication {
    public static void main(String[] args) throws InterruptedException {
        ILeaderboardService service = LeaderboardService.getInstance();
        IGameRepository repository = InMemoryGameRepository.getInstance();
        ExpiryHandler expiryHandler = DefaultExpiryHandler.getInstance();

        System.out.println("========== CASE 1: LOW TRAFFIC – HAPPY PATH ==========");
        runLowTrafficHappyPath(service, repository, expiryHandler);

        System.out.println("\n========== CASE 2: HIGH PARALLEL TRAFFIC – CONCURRENT SCORE SUBMISSION ==========");
        runParallelTraffic(service);

        BackgroundExpiryRunner runner = BackgroundExpiryRunner.getInstance(60);
        runner.start();
        System.out.println("\nBackground expiry runner started (interval 60s).");
    }

    private static void runLowTrafficHappyPath(ILeaderboardService service, IGameRepository repository,
                                               ExpiryHandler expiryHandler) {
        long base = System.currentTimeMillis() / 1000;
        long start = base - 3600;
        long end = base + 86400;

        String lb1 = service.createLeaderboard("game1", "Snake Game", "Daily Leaderboard", CampaignType.DAILY, start, end);
        String lb2 = service.createLeaderboard("game1", "Snake Game", "Short Run", CampaignType.WEEKLY, start, base + 3);
        System.out.println("Created leaderboards: " + lb1 + ", " + lb2);

        service.submitScore("game1", "u1", 100, base);
        service.submitScore("game1", "u2", 200, base);
        service.submitScore("game1", "u3", 150, base);
        service.submitScore("game1", "u1", 180, base + 1);

        System.out.println("getLeaderboard(lb1):");
        for (ScoreEntry e : service.getLeaderboard(lb1)) {
            System.out.println("  " + e.getUserId() + " -> " + e.getScore());
        }

        repository.runExpiryCheck(base + 10, expiryHandler);
        CampaignResult result = service.getCampaignResult(lb2);
        if (result != null) {
            System.out.println("Campaign result (lb2): winner=" + result.getWinnerUserId() + ", score=" + result.getWinnerScore() + ", status=" + result.getRewardStatus());
            service.markRewardDisbursed(lb2);
            System.out.println("After markRewardDisbursed: " + service.getCampaignResult(lb2).getRewardStatus());
        }

        System.out.println("listPlayersPrev(game1, lb1, u3, 2):");
        service.listPlayersPrev("game1", lb1, "u3", 2).forEach(e -> System.out.println("  " + e.getUserId() + " " + e.getScore()));
        System.out.println("listPlayersNext(game1, lb1, u3, 2):");
        service.listPlayersNext("game1", lb1, "u3", 2).forEach(e -> System.out.println("  " + e.getUserId() + " " + e.getScore()));

        System.out.println("getSupportedGames(): " + service.getSupportedGames());
    }

    private static void runParallelTraffic(ILeaderboardService service) throws InterruptedException {
        long base = System.currentTimeMillis() / 1000;
        long start = base - 3600;
        long end = base + 86400;

        String lb = service.createLeaderboard("game2", "Ludo", "Parallel Test", CampaignType.DAILY, start, end);
        System.out.println("Created: " + lb);

        ExecutorService executor = Executors.newFixedThreadPool(10);
        for (int i = 0; i < 100; i++) {
            final int idx = i;
            executor.submit(() -> service.submitScore("game2", "user_" + idx, 100 + idx % 500, base));
        }
        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);

        List<ScoreEntry> ranked = service.getLeaderboard(lb);
        System.out.println("Leaderboard size: " + ranked.size());
        System.out.println("Top 5: " + ranked.stream().limit(5).map(e -> e.getUserId() + "=" + e.getScore()).toList());
    }
}
