package jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.junit.Test;

import java.util.Date;

public class JwtTest {
    
    /****
     * 创建Jwt令牌
     */
    @Test
    public void testCreateJwt(){
        JwtBuilder builder = Jwts.builder();
        builder.setId("888");//设置唯一编号
        builder.setSubject("夏超");//设置主题
        builder.setIssuedAt(new Date());
        builder.setExpiration(new Date(System.currentTimeMillis()+3600000));
        builder.signWith(SignatureAlgorithm.HS256,"itcast");
        System.out.println(builder.compact());
    }

    /***
     * 解析Jwt令牌数据
     */
    @Test
    public void testParseJwt(){
        String compactJwt = "eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiI4ODgiLCJzdWIiOiLlpI_otoUiLCJ" +
                "pYXQiOjE2MjkxOTIwMzgsImV4cCI6MTYyOTE5MjA1OH0.xnyK_i4Y9D_ZMm7J6KnodqI_g3pgbaETOoJf8g9p86E";
        Claims claims = Jwts.parser().setSigningKey("itcast")
                .parseClaimsJws(compactJwt)
                .getBody();
        System.out.println(claims.toString());
    }
}
