package com.mjduan.project.example1.test;


import org.junit.Test;

import com.mjduan.project.example1.src.Request;

/**
 * Hans  2017-06-21 22:43
 */
public class LombokTest {

    @Test
    public void test(){
        Request request = Request.builder().name("msg").build();
        System.out.println(request.toString());
    }

}
