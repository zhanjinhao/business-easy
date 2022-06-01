package cn.addenda.businesseasy.asynctask;

import java.util.concurrent.CompletableFuture;

/**
 * @author 01395265
 * @date 2022/5/24
 */
public class TelGenerator {

    private long g1Mills;
    private long g2Mills;
    private long g3Mills;
    private long g4Mills;
    private long b1Mills;
    private long b2Mills;
    private long b3Mills;

    public TelGenerator(int g1Seconds, int g2Seconds, int g3Seconds, int g4Seconds, int b1Seconds, int b2Seconds, int b3Seconds) {
        this.g1Mills = g1Seconds * 1000L;
        this.g2Mills = g2Seconds * 1000L;
        this.g3Mills = g3Seconds * 1000L;
        this.g4Mills = g4Seconds * 1000L;
        this.b1Mills = b1Seconds * 1000L;
        this.b2Mills = b2Seconds * 1000L;
        this.b3Mills = b3Seconds * 1000L;
    }

    public String generate() {

        CompletableFuture<FutureResult<String>> group1Future = AsyncTaskExecutorService.supplyAsync(() -> new FutureResult<>(group1()));

        // 中间数据直接提交
        CompletableFuture<FutureResult<Basic1>> basic1Future = AsyncTaskExecutorService.supplyAsync(() -> new FutureResult<>(basic1()));

        // basic1Future complete的时候才能执行 group2()
        CompletableFuture<FutureResult<String>> group2Future =
            AsyncTaskExecutorService.thenApplyAsync(basic1Future, basic1FutureResult -> {
                if (basic1FutureResult.isSuccess()) {
                    return new FutureResult<>(basic1FutureResult.getResult().getValue() + group2());
                } else {
                    return new FutureResult<String>(basic1FutureResult.getThrowableList());
                }
            });

        CompletableFuture<FutureResult<Basic2>> basic2Future = AsyncTaskExecutorService.supplyAsync(() -> new FutureResult<>(basic2()));

        CompletableFuture<FutureResult<BinaryResult<Basic1, Basic2>>> basic1AndBasic2Future =
            AsyncTaskExecutorService.thenCombineAsync(basic1Future, basic2Future,
                (basic1FutureResult, basic2FutureResult) -> {
                    if (basic1FutureResult.isSuccess() && basic2FutureResult.isSuccess()) {
                        return new FutureResult<>(new BinaryResult<>(basic1FutureResult.getResult(), basic2FutureResult.getResult()));
                    } else {
                        return FutureResult.combineThrowableList(basic1FutureResult, basic2FutureResult);
                    }
                });

        CompletableFuture<FutureResult<String>> group3Future =
            AsyncTaskExecutorService.thenApplyAsync(basic1AndBasic2Future,
                binaryResultFutureResult -> {
                    if (binaryResultFutureResult.isSuccess()) {
                        final BinaryResult<Basic1, Basic2> binaryResult = binaryResultFutureResult.getResult();
                        return new FutureResult<>(binaryResult.getFirstResult().getValue() + binaryResult.getFirstResult().getValue() + group3());
                    } else {
                        return binaryResultFutureResult.convertTypeWithThrowableList(String.class);
                    }
                });

        CompletableFuture<FutureResult<Basic3>> basic3Future = AsyncTaskExecutorService.supplyAsync(() -> new FutureResult<>(basic3()));

        CompletableFuture<FutureResult<String>> group4Future =
            AsyncTaskExecutorService.thenCombineAsync(basic1AndBasic2Future, basic3Future,
                (binaryResultFutureResult, basic3FutureResult) -> {
                    if (binaryResultFutureResult.isSuccess()) {
                        final BinaryResult<Basic1, Basic2> binaryResult = binaryResultFutureResult.getResult();
                        return new FutureResult<>(binaryResult.getFirstResult().getValue()
                            + binaryResult.getSecondResult().getValue() + basic3FutureResult.getResult().getValue() + group4());
                    } else {
                        return binaryResultFutureResult.convertTypeWithThrowableList(String.class);
                    }
                });

        AsyncTaskExecutorService.allOfComplete(group1Future, group2Future, group3Future, group4Future);

        Tel tel = new Tel();
        tel.setGroup1(AsyncTaskExecutorService.retrieveResultNow(group1Future));
        tel.setGroup2(AsyncTaskExecutorService.retrieveResultNow(group2Future));
        tel.setGroup3(AsyncTaskExecutorService.retrieveResultNow(group3Future));
        tel.setGroup4(AsyncTaskExecutorService.retrieveResultNow(group4Future));
        return tel.assembleTel();
    }

    private String group1() {
        sleep(g1Mills);
//        int i = 1 / 0;
        System.out.println("group1 complete!");
        return "group1";
    }

    private String group2() {
        sleep(g2Mills);
        System.out.println("group2 complete!");
        return "group2";
    }

    private String group3() {
        sleep(g3Mills);
        System.out.println("group3 complete!");
        return "group3";
    }

    private String group4() {
        sleep(g4Mills);
        System.out.println("group4 complete!");
        return "group4";
    }

    private Basic1 basic1() {
        sleep(b1Mills);
        System.out.println("basic1 complete!");
        return new Basic1("basic1");
    }

    private Basic2 basic2() {
        sleep(b2Mills);
        System.out.println("basic2 complete!");
        return new Basic2("basic2");
    }

    private Basic3 basic3() {
        sleep(b3Mills);
        System.out.println("basic3 complete!");
        return new Basic3("basic3");
    }

    private static void sleep(long mills) {
        try {
            Thread.sleep(mills);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
