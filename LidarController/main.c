#include <msp430.h> 

/*
 * LIDARcontroller:
 * interfaces with the serial port of an XV-11 LIDAR module
 * controls the motor speed via PWM and reports data as an SPI slave
 *
 * Author: Edward Shin
 */

const unsigned long smclk_freq = 16000000;
const unsigned long bps = 115200;


//Serial Debug Variables
volatile char line[11];
volatile char counter = 0;


int main(void) {
    WDTCTL = WDTPW | WDTHOLD;	// Stop watchdog timer

    /* Use Calibration values  for 16MHz    Clock   DCO*/
    DCOCTL = 0;
    BCSCTL1 = CALBC1_16MHZ;
    DCOCTL = CALDCO_16MHZ;

    /*
     *  UART Configuration
     */

    /* Configure Pin Muxes for P1.1 RX; P2.2 TX */
    P1DIR = (~BIT1) & BIT2;


    P1SEL = BIT1 | BIT2;
    P1SEL2 = BIT1 | BIT2;

    /*
     * PWM Configuration
     */

    P2DIR |= BIT6; // Pin 2.6 Output

    P2SEL = BIT6; // Pin 2.6 to TA0.1 for PWM

    TA0CCR0 = 1000; // PWM Period
    TA0CCTL1 = OUTMOD_7; // CCR1 reset/set
    TA0CCR1 = 375; // CCR1 PWM duty cycle
    TACTL = TASSEL_2 + MC_1; // SMCLK, up mode

    static char P = 1;
    static char I = 0;
    static char D = 0;
    static char neutral = 375;
	static int setpoint = 15015;
    for(;;)
    {


    }

	return 0;
}





