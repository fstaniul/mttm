package com.staniul.util;

import com.staniul.util.lang.StringUtil;
import org.junit.Test;

import static org.junit.Assert.*;

public class StringUtilTest {
    @Test
    public void splitOnSize() throws Exception {
        String test = "To jest super test tego czy to dziala!";
        String[] result = {"To jest", "super test", "tego czy", "to dziala!"};
        assertArrayEquals(result, StringUtil.splitOnSize(test, " ", 10));
    }

    @Test
    public void splitOnSize1() throws Exception {
        String test = "Willy";
        String[] expected = {"W", "i", "l", "l", "y"};
        String[] actual = StringUtil.splitOnSize(test, " ", 1);
        //System.out.println(Arrays.toString(actual));
        assertArrayEquals(expected, actual);
    }

}