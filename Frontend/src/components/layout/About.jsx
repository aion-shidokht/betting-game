import React from 'react';
import Typography from '@material-ui/core/Typography';
import Link from "@material-ui/core/Link";

export default function About() {
  return (
    <div>
      <Typography variant="h6" gutterBottom>
        1. Registration
      </Typography>
      <Typography variant="body1" gutterBottom>
        You need to create an account on the Amity network, and register as a player in the game's smart contract.
        For information on creating an account, you can refer to <Link href="https://getaiwa.com/">aiwa</Link> or

          <Link href="https://validators.theoan.com/docs/account-management">oan</Link> documentation.
        Once the account has been created, simply enter its information in the form link provided in the announcement
        message and the registration will be done for you.
      </Typography>


      <Typography variant="h6" gutterBottom>
        2. Submitting a statement
      </Typography>
      <Typography variant="body1" gutterBottom>
        Write up a statement you want to submit, and select the OAN employee the statement refers to from the drop-down
        menu. You also need to provide a salt when you submit a statement. You must remember this salt until it's time
        to reveal your answer.
        Your salt can be any set of characters (with the length of at least 1). It can be reused for all your statement
        submissions.
      </Typography>


      <Typography variant="h6" gutterBottom>
        3. Making guesses
      </Typography>
      <Typography variant="body1" gutterBottom>
        To make a guess, view the list of statements from <Link href="http://whosaidthat.theoandev.com/statements">
        all statements
      </Link>, and simply select
        the person you think that statement refers to. You can only make a single guess per statement.

      </Typography>

      <Typography variant="h6" gutterBottom>
        4. Reveal your answer
      </Typography>
      <Typography variant="body1" gutterBottom>
        Once the game is closed, it's time to reveal your answers! To do so, find your statement in <span/>
        <Link href="http://whosaidthat.theoandev.com/statements">
          all statements
        </Link>. You will need to provide the salt you provided when submitting your
        statement.
      </Typography>

      <Typography style={{paddingTop: 10}} variant="body1" gutterBottom>
        Whoever guesses the most correct answers to statements will win the game. Winners will be announced on Friday the 13th.
      </Typography>
    </div>
  );
}
