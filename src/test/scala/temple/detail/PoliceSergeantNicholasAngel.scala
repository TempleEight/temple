package temple.detail

/**
  * Police Constable Nicholas Angel: born and schooled in London,
  * graduated Canterbury University in 1993 with a double first in Politics and Sociology. Attended Hendon College of
  * Police Training. Displayed great aptitude in field exercises, notably Urban Pacification and Riot Control.
  * Academically excelled in theoretical course work and final year examinations. Received a Baton of Honour,
  * graduated with distinction into the Metropolitan Police Service and quickly established an effectiveness and
  * popularity within the community. Proceeded to improve skill base with courses in advanced driving... and advanced
  * cycling. He became heavily involved in a number of extra-vocational activities and to this day, he holds the Met
  * record for the hundred metre dash. In 2001, he began active duty with the renowned SO19 Armed Response Unit and
  * received a Bravery Award for efforts in the resolution of Operation Crackdown. In the last twelve months, he has
  * received nine special commendations, achieved highest arrest record for any officer in the Met and sustained three
  * injuries in the line of duty, most recently in December when wounded by a man dressed as Father Christmas.
 **/
object PoliceSergeantNicholasAngel extends QuestionAsker {

  /** Police Sergeant Nicholas Angel loves asking questions to the public in order to find out the answers he needs */
  override def askQuestion(question: String): String =
    if (question.contains("module")) "github.com/squat/and/dab" else "Y"
}
